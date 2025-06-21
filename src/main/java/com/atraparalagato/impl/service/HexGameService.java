package com.atraparalagato.impl.service;

import com.atraparalagato.base.service.GameService;
import com.atraparalagato.base.model.GameState;
import com.atraparalagato.base.model.GameBoard;
import com.atraparalagato.base.strategy.CatMovementStrategy;
import com.atraparalagato.example.strategy.SimpleCatMovement;
import com.atraparalagato.impl.strategy.AStarCatMovement;
import com.atraparalagato.impl.strategy.BFSCatMovement;
import com.atraparalagato.impl.model.HexPosition;
import com.atraparalagato.impl.repository.H2GameRepository;
import com.atraparalagato.impl.model.HexGameState;
import com.atraparalagato.impl.model.HexGameBoard;

import java.util.*;




/**
 * Implementación esqueleto de GameService para el juego hexagonal.
 * 
 * Los estudiantes deben completar los métodos marcados con TODO.
 * 
 * Conceptos a implementar:
 * - Orquestación de todos los componentes del juego
 * - Lógica de negocio compleja
 * - Manejo de eventos y callbacks
 * - Validaciones avanzadas
 * - Integración con repositorio y estrategias
 */
public class HexGameService extends GameService<HexPosition> {
    //DataRepository<HexGameState, String> repo = new H2GameRepository();
    // TODO: Los estudiantes deben inyectar dependencias
    // Ejemplos: repository, movementStrategy, validator, etc.
    private static final int DEFAULT_BOARD_SIZE = 5;
    private final H2GameRepository repo = new H2GameRepository();
    private final Set<String> pausedGames = new HashSet<>();

    public HexGameService() {
        super(
            new HexGameBoard(DEFAULT_BOARD_SIZE),
            new SimpleCatMovement(new HexGameBoard(DEFAULT_BOARD_SIZE)),
            new H2GameRepository(),
            () -> UUID.randomUUID().toString(),
            HexGameBoard::new,
            id -> new HexGameState(id, DEFAULT_BOARD_SIZE)
        );
    }
    
    /**
     * TODO: Crear un nuevo juego con configuración personalizada.
     * Debe ser más sofisticado que ExampleGameService.
     */
    public HexGameState createGame(int boardSize, String difficulty, Map<String, Object> options) {
        String gameId = UUID.randomUUID().toString();
        HexGameState gameState = new HexGameState(gameId, boardSize);

        gameState.setOnStateChanged(this::onGameStateChanged);
        gameState.setOnGameEnded(this::onGameEnded);

        repo.save(gameState);

        System.out.println("🎮 Nuevo juego iniciado: " + gameId + " dificultad:" + difficulty);
        return gameState;
    }
    
    /**
     * TODO: Ejecutar movimiento del jugador con validaciones avanzadas.
     */
    public Optional<HexGameState> executePlayerMove(String gameId, HexPosition position, String playerId) {
       Optional<HexGameState> gameStateOpt = repo.findById(gameId);
        
        if (gameStateOpt.isEmpty()) {
            return Optional.empty();
        }
        
        HexGameState gameState = gameStateOpt.get();
        
        // Validar movimiento
        if (!isValidMove(gameState.getGameId(), position)) {
            return Optional.of(gameState);
        }
        
        // Ejecutar movimiento del jugador
        if (!gameState.executeMove(position)) {
            return Optional.of(gameState);
        }
        
        System.out.println("👤 Jugador bloqueó posición: " + position + " (Movimiento #" + gameState.getMoveCount() + ")");
        
        // Mover el gato después del movimiento del jugador
        executeCatMove(gameState);
        
        // Guardar estado actualizado
        repo.save(gameState);
        
        return Optional.of(gameState);
    }
    
    /**
     * TODO: Obtener estado del juego con información enriquecida.
     */
    public Optional<Map<String, Object>> getEnrichedGameState(String gameId) {
        Optional<HexGameState> stateOpt = repo.findById(gameId);
        if (stateOpt.isEmpty()) {
            return Optional.empty();
        }

        HexGameState state = stateOpt.get();
        Map<String, Object> result = new HashMap<>();
        result.put("state", state.getSerializableState());
        result.put("advancedStats", state.getAdvancedStatistics());
        result.put("suggestion", getSuggestedMove(gameId)
                .map(p -> Map.of("q", p.getQ(), "r", p.getR()))
                .orElse(null));
        result.put("analysis", analyzeGame(gameId));
        result.put("board", state.getGameBoard().getBoardStatistics());

        return Optional.of(result);
    }
    
    /**
     * TODO: Obtener sugerencia inteligente de movimiento.
     */
    public Optional<HexPosition> getIntelligentSuggestion(String gameId, String difficulty) {
        Optional<HexGameState> stateOpt = repo.findById(gameId);
        if (stateOpt.isEmpty()) {
            return Optional.empty();
        }

        HexGameState state = stateOpt.get();
        HexGameBoard board = state.getGameBoard();
        CatMovementStrategy<HexPosition> strategy = createMovementStrategy(difficulty, board);

        HexPosition current = state.getCatPosition();
        HexPosition target = getTargetPosition(state);
        Optional<HexPosition> catMove = strategy.findBestMove(current, target);
        if (catMove.isPresent()) {
            return catMove; // sugerir bloquear la posición a la que se movería el gato
        }
        return getSuggestedMove(gameId);
    }
    
    /**
     * TODO: Analizar la partida y generar reporte.
     */
    public Map<String, Object> analyzeGame(String gameId) {
        Optional<HexGameState> stateOpt = repo.findById(gameId);
        if (stateOpt.isEmpty()) {
            return Map.of();
        }
        HexGameState state = stateOpt.get();

        Map<String, Object> analysis = new HashMap<>();
        analysis.put("moveCount", state.getMoveCount());
        analysis.put("playerWon", state.hasPlayerWon());
        analysis.put("score", state.calculateScore());
        analysis.put("boardStats", state.getGameBoard().getBoardStatistics());
        return analysis;
    }
    
    /**
     * TODO: Obtener estadísticas globales del jugador.
     */
    public Map<String, Object> getPlayerStatistics(String playerId) {
        // Este repositorio no guarda información del jugador; se usan estadísticas generales
        List<HexGameState> games = repo.findAll();
        int total = games.size();
        long won = games.stream().filter(HexGameState::hasPlayerWon).count();
        double winRate = total == 0 ? 0.0 : (double) won * 100 / total;
        double avgScore = games.stream().mapToInt(HexGameState::calculateScore).average().orElse(0.0);

        Map<String, Object> stats = new HashMap<>();
        stats.put("playerId", playerId);
        stats.put("gamesPlayed", total);
        stats.put("gamesWon", won);
        stats.put("winRate", winRate);
        stats.put("averageScore", avgScore);
        return stats;
    }
    
    /**
     * TODO: Configurar dificultad del juego.
     */
    public void setGameDifficulty(String gameId, String difficulty) {
        repo.findById(gameId).ifPresent(state -> {
            System.out.println("Cambiando dificultad a " + difficulty + " para " + gameId);
        });
    }
    
    /**
     * TODO: Pausar/reanudar juego.
     */
    public boolean toggleGamePause(String gameId) {
        if (pausedGames.contains(gameId)) {
            pausedGames.remove(gameId);
            return false;
        } else {
            pausedGames.add(gameId);
            return true;
        }
    }
    
    /**
     * TODO: Deshacer último movimiento.
     */
    public Optional<HexGameState> undoLastMove(String gameId) {
        // Funcionalidad no implementada, se retorna estado actual
        return repo.findById(gameId);
    }
    
    /**
     * TODO: Obtener ranking de mejores puntuaciones.
     */
    public List<Map<String, Object>> getLeaderboard(int limit) {
        List<HexGameState> games = repo.findAllSorted(HexGameState::calculateScore, false);
        return games.stream().limit(limit).map(g -> {
            Map<String, Object> data = new HashMap<>();
            data.put("gameId", g.getGameId());
            data.put("score", g.calculateScore());
            data.put("createdAt", g.getCreatedAt());
            data.put("playerWon", g.hasPlayerWon());
            return data;
        }).toList();
    }
    
    // Métodos auxiliares que los estudiantes pueden implementar
    
    /**
     * TODO: Validar movimiento según reglas avanzadas.
     */
    private boolean isValidAdvancedMove(HexGameState gameState, HexPosition position, String playerId) {
        if (gameState.isGameFinished()) return false;
        HexGameBoard board = gameState.getGameBoard();
        return board.isPositionInBounds(position) && !board.isBlocked(position) && !position.equals(gameState.getCatPosition());
    }
    
    /**
     * TODO: Ejecutar movimiento del gato usando estrategia apropiada.
     */
    private void executeCatMove(HexGameState gameState, String difficulty) {
        HexGameBoard board = gameState.getGameBoard();
        CatMovementStrategy<HexPosition> strategy = createMovementStrategy(difficulty, board);
        HexPosition currentPosition = gameState.getCatPosition();
        HexPosition targetPosition = getTargetPosition(gameState);
        strategy.findBestMove(currentPosition, targetPosition)
                .ifPresent(gameState::setCatPosition);
    }
    
    /**
     * TODO: Calcular puntuación avanzada.
     */
    private int calculateAdvancedScore(HexGameState gameState, Map<String, Object> factors) {
        int base = gameState.calculateScore();
        int bonus = 0;
        if (factors != null && Boolean.TRUE.equals(factors.get("fast"))) {
            bonus += 50;
        }
        return base + bonus;
    }
    
    /**
     * TODO: Notificar eventos del juego.
     */
    private void notifyGameEvent(String gameId, String eventType, Map<String, Object> eventData) {
        System.out.println("Evento " + eventType + " en juego " + gameId + " -> " + eventData);
    }
    
    /**
     * TODO: Crear factory de estrategias según dificultad.
     */
    private CatMovementStrategy createMovementStrategy(String difficulty, HexGameBoard board) {
        if ("hard".equalsIgnoreCase(difficulty)) {
            return new AStarCatMovement(board);
        }
        return new BFSCatMovement(board);
    }

    // Métodos abstractos requeridos por GameService
    
    @Override
    protected void initializeGame(GameState<HexPosition> gameState, GameBoard<HexPosition> gameBoard) {
        if (gameState instanceof HexGameState hexState && gameBoard instanceof HexGameBoard board) {
            hexState.setCatPosition(new HexPosition(0, 0));
        }
    }
    
    @Override
    public boolean isValidMove(String gameId, HexPosition position) {
        Optional<HexGameState> gameStateOpt = repo.findById(gameId);
        if (gameStateOpt.isEmpty()) return false;
        HexGameState gameState = gameStateOpt.get();

        if (gameState.isGameFinished()) return false;
        if (position.equals(gameState.getCatPosition())) return false;

        HexGameBoard board = gameState.getGameBoard();
        return board.isPositionInBounds(position) && !board.isBlocked(position);
    }
    
    @Override
    public Optional<HexPosition> getSuggestedMove(String gameId) {
        Optional<HexGameState> gameStateOpt = repo.findById(gameId);
        
        if (gameStateOpt.isEmpty()) {
            return Optional.empty();
        }
        
        HexGameState gameState = gameStateOpt.get();
        HexPosition catPosition = gameState.getCatPosition();
        HexGameBoard board = gameState.getGameBoard();
        
        // Sugerencia simple: bloquear una posición adyacente al gato
        List<HexPosition> adjacentToCat = board.getAdjacentPositions(catPosition);
        
        return adjacentToCat.stream()
                .filter(pos -> !board.isBlocked(pos))
                .findFirst();
    }
    
    @Override
    protected HexPosition getTargetPosition(GameState<HexPosition> gameState) {
        if (gameState instanceof HexGameState hexState) {
            int size = hexState.getBoardSize();
            return new HexPosition(size, 0);
        }
        return new HexPosition(0, 0);
    }
    
    @Override
    public Object getGameStatistics(String gameId) {
        Optional<HexGameState> gameStateOpt = repo.findById(gameId);
        
        if (gameStateOpt.isEmpty()) {
            return Map.of("error", "Game not found");
        }
        
        HexGameState gameState = gameStateOpt.get();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("gameId", gameState.getGameId());
        stats.put("status", gameState.getStatus().toString());
        stats.put("moveCount", gameState.getMoveCount());
        stats.put("isFinished", gameState.isGameFinished());
        stats.put("playerWon", gameState.hasPlayerWon());
        stats.put("score", gameState.calculateScore());
        stats.put("createdAt", gameState.getCreatedAt());
        stats.put("boardSize", gameState.getBoardSize());
        stats.put("catPosition", Map.of(
            "q", gameState.getCatPosition().getQ(),
            "r", gameState.getCatPosition().getR()
        ));
        
        // Estadísticas del tablero
        stats.put("boardStats", gameState.getGameBoard().getBoardStatistics());
        
        return stats;
    }
} 