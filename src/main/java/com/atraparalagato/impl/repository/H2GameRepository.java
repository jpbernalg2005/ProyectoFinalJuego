package com.atraparalagato.impl.repository;

import com.atraparalagato.base.repository.DataRepository;
import com.atraparalagato.impl.model.HexGameState;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class H2GameRepository extends DataRepository<HexGameState, String> {

    private final Connection connection;
    private final ObjectMapper mapper = new ObjectMapper();

    public H2GameRepository() {
        try {
            // Puedes cambiar la URL si quieres persistencia en disco
            connection = DriverManager.getConnection("jdbc:h2:mem:gatodb;DB_CLOSE_DELAY=-1", "sa", "");
            createSchema();
        } catch (SQLException e) {
            throw new RuntimeException("Error al conectar a H2", e);
        }
    }

    private void createSchema() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS hex_game_state (
                game_id VARCHAR(255) PRIMARY KEY,
                data CLOB
            )
        """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    @Override
    public HexGameState save(HexGameState entity) {
        if (entity == null) throw new IllegalArgumentException("Entity cannot be null");
        beforeSave(entity);
        try {
            String json = serializeGameState(entity);
            PreparedStatement stmt = connection.prepareStatement(
                "MERGE INTO hex_game_state (game_id, data) KEY(game_id) VALUES (?, ?)");
            stmt.setString(1, entity.getGameId());
            stmt.setString(2, json);
            stmt.executeUpdate();
            afterSave(entity);
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Error al guardar HexGameState", e);
        }
    }

    @Override
    public Optional<HexGameState> findById(String id) {
        if (id == null) return Optional.empty();
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT data FROM hex_game_state WHERE game_id = ?");
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String json = rs.getString("data");
                return Optional.of(deserializeGameState(json, id));
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al buscar por ID", e);
        }
    }

    @Override
    public List<HexGameState> findAll() {
        try {
            List<HexGameState> list = new ArrayList<>();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT game_id, data FROM hex_game_state");
            while (rs.next()) {
                list.add(deserializeGameState(rs.getString("data"), rs.getString("game_id")));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener todos", e);
        }
    }

    @Override
    public List<HexGameState> findWhere(Predicate<HexGameState> condition) {
        // Simple: carga todos y filtra en memoria (igual que el ejemplo)
        return findAll().stream().filter(condition).collect(Collectors.toList());
    }

    @Override
    public <R> List<R> findAndTransform(Predicate<HexGameState> condition, Function<HexGameState, R> transformer) {
        return findWhere(condition).stream().map(transformer).collect(Collectors.toList());
    }

    @Override
    public long countWhere(Predicate<HexGameState> condition) {
        return findWhere(condition).stream().count();
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null) return false;
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM hex_game_state WHERE game_id = ?");
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar por ID", e);
        }
    }

    @Override
    public long deleteWhere(Predicate<HexGameState> condition) {
        List<HexGameState> toDelete = findWhere(condition);
        long deletedCount = 0;
        for (HexGameState g : toDelete) {
            if (deleteById(g.getGameId())) deletedCount++;
        }
        return deletedCount;
    }

    @Override
    public boolean existsById(String id) {
        if (id == null) return false;
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM hex_game_state WHERE game_id = ?");
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar existencia", e);
        }
    }

    @Override
    public <R> R executeInTransaction(Function<DataRepository<HexGameState, String>, R> operation) {
        try {
            connection.setAutoCommit(false);
            R result = operation.apply(this);
            connection.commit();
            return result;
        } catch (Exception e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                System.err.println("Error en rollback: " + ex.getMessage());
            }
            throw new RuntimeException("Transaction failed", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {}
        }
    }

    @Override
    public List<HexGameState> findWithPagination(int page, int size) {
        if (page < 0 || size <= 0) return Collections.emptyList();
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT game_id, data FROM hex_game_state LIMIT ? OFFSET ?");
            stmt.setInt(1, size);
            stmt.setInt(2, page * size);
            ResultSet rs = stmt.executeQuery();
            List<HexGameState> list = new ArrayList<>();
            while (rs.next()) {
                list.add(deserializeGameState(rs.getString("data"), rs.getString("game_id")));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("Error en paginación", e);
        }
    }

    @Override
    public List<HexGameState> findAllSorted(Function<HexGameState, ? extends Comparable<?>> sortKeyExtractor, boolean ascending) {
        Comparator<HexGameState> comparator = (a, b) -> {
            Comparable keyA = sortKeyExtractor.apply(a);
            Comparable keyB = sortKeyExtractor.apply(b);
            if (keyA == null && keyB == null) return 0;
            if (keyA == null) return ascending ? -1 : 1;
            if (keyB == null) return ascending ? 1 : -1;
            return ascending ? keyA.compareTo(keyB) : keyB.compareTo(keyA);
        };
        return findAll().stream().sorted(comparator).collect(Collectors.toList());
    }

    @Override
    public <R> List<R> executeCustomQuery(String query, Function<Object, R> resultMapper) {
        // Ejemplo simple: solo soporta queries SQL que devuelvan game_id y data
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            List<R> result = new ArrayList<>();
            while (rs.next()) {
                String data = null;
                String gameId = null;
                try {
                    data = rs.getString("data");
                    gameId = rs.getString("game_id");
                    HexGameState state = deserializeGameState(data, gameId);
                    result.add(resultMapper.apply(state));
                } catch (Exception ex) {
                    System.err.println("Error deserializando estado del juego (game_id=" + gameId + "): " + ex.getMessage());
                    // Puedes decidir si saltar el registro o lanzar una excepción
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error ejecutando consulta personalizada", e);
        }
    }

    @Override
    protected void initialize() {
        // Ya se inicializa en el constructor
    }

    @Override
    protected void cleanup() {
        try {
            connection.close();
        } catch (SQLException e) {
            System.err.println("Error cerrando conexión");
        }
    }

    @Override
    protected boolean validateEntity(HexGameState entity) {
        return entity != null && entity.getGameId() != null && !entity.getGameId().trim().isEmpty();
    }

    @Override
    protected void beforeSave(HexGameState entity) {
        if (!validateEntity(entity)) {
            throw new IllegalArgumentException("Invalid game state entity");
        }
        System.out.println("Guardando juego: " + entity.getGameId());
    }

    @Override
    protected void afterSave(HexGameState entity) {
        System.out.println("Juego guardado exitosamente: " + entity.getGameId());
    }

    // Serialización y deserialización usando Jackson
    private String serializeGameState(HexGameState gameState) throws Exception {
        return mapper.writeValueAsString(gameState);
    }

    private HexGameState deserializeGameState(String serializedData, String gameId) throws Exception {
        HexGameState state = mapper.readValue(serializedData, HexGameState.class);
        // Si necesitas setear el gameId manualmente, hazlo aquí
        return state;
    }
}