package com.atraparalagato.impl.strategy;

import com.atraparalagato.base.model.GameBoard;
import com.atraparalagato.base.strategy.CatMovementStrategy;
import com.atraparalagato.impl.model.HexPosition;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Implementación esqueleto de estrategia BFS (Breadth-First Search) para el gato.
 * 
 * Los estudiantes deben completar los métodos marcados con TODO.
 * 
 * Conceptos a implementar:
 * - Algoritmo BFS para pathfinding
 * - Exploración exhaustiva de caminos
 * - Garantía de encontrar el camino más corto
 * - Uso de colas para exploración por niveles
 */
public class BFSCatMovement extends CatMovementStrategy<HexPosition> {
    
    public BFSCatMovement(GameBoard<HexPosition> board) {
        super(board);
    }
    
    @Override
    protected List<HexPosition> getPossibleMoves(HexPosition currentPosition) {
        // TODO: Obtener todas las posiciones adyacentes válidas
        // Filtrar posiciones bloqueadas y fuera de límites
        throw new UnsupportedOperationException("Los estudiantes deben implementar getPossibleMoves");
    }
    
    @Override
    protected Optional<HexPosition> selectBestMove(List<HexPosition> possibleMoves, 
                                                  HexPosition currentPosition, 
                                                  HexPosition targetPosition) {
        // TODO: Usar BFS para encontrar el mejor movimiento
        // 1. Ejecutar BFS desde cada posible movimiento
        // 2. Evaluar cuál lleva más rápido al objetivo
        // 3. Retornar el primer paso del mejor camino
        throw new UnsupportedOperationException("Los estudiantes deben implementar selectBestMove");
    }
    
    @Override
    protected Function<HexPosition, Double> getHeuristicFunction(HexPosition targetPosition) {
        // TODO: BFS no necesita heurística, pero puede usarse para desempate
        // Retornar función que calcule distancia euclidiana o Manhattan
        throw new UnsupportedOperationException("Los estudiantes deben implementar getHeuristicFunction");
    }
    
    @Override
    protected Predicate<HexPosition> getGoalPredicate() {
        // TODO: Definir condición de objetivo (llegar al borde)
        throw new UnsupportedOperationException("Los estudiantes deben implementar getGoalPredicate");
    }
    
    @Override
    protected double getMoveCost(HexPosition from, HexPosition to) {
        // TODO: BFS usa costo uniforme (1.0 para movimientos adyacentes)
        throw new UnsupportedOperationException("Los estudiantes deben implementar getMoveCost");
    }
    
    @Override
    public boolean hasPathToGoal(HexPosition currentPosition) {
        // TODO: Implementar BFS para verificar si existe camino al objetivo
        // 1. Usar cola para exploración por niveles
        // 2. Marcar posiciones visitadas
        // 3. Retornar true si se encuentra el objetivo
        throw new UnsupportedOperationException("Los estudiantes deben implementar hasPathToGoal");
    }
    
    @Override
    public List<HexPosition> getFullPath(HexPosition currentPosition, HexPosition targetPosition) {
        // TODO: Implementar BFS completo para encontrar camino
        // 1. Usar cola con información de camino
        // 2. Reconstruir camino desde objetivo hasta inicio
        // 3. Retornar camino completo
        throw new UnsupportedOperationException("Los estudiantes deben implementar getFullPath");
    }
    
    // Métodos auxiliares que los estudiantes pueden implementar

    /**
     * Ejecutar BFS desde una posición hasta encontrar objetivo.
     */
    private Optional<List<HexPosition>> bfsToGoal(HexPosition start) {
        Set<HexPosition> visited = new HashSet<>();
        Queue<HexPosition> queue = new LinkedList<>();
        Map<HexPosition, HexPosition> parentMap = new HashMap<>();

        queue.offer(start);
        visited.add(start);
        parentMap.put(start, null);

        Predicate<HexPosition> goalPredicate = getGoalPredicate();

        while (!queue.isEmpty()) {
            HexPosition current = queue.poll();

            if (goalPredicate.test(current)) {
                // Reconstruir camino desde start hasta current
                return Optional.of(reconstructPath(parentMap, start, current));
            }

            for (HexPosition neighbor : board.getAdjacentPositions(current)) {
                if (!visited.contains(neighbor) && !board.isBlocked(neighbor)) {
                    visited.add(neighbor);
                    parentMap.put(neighbor, current);
                    queue.offer(neighbor);
                }
            }
        }
        return Optional.empty(); // No se encontró camino
    }

    /**
     * Reconstruir camino desde mapa de padres.
     */
    private List<HexPosition> reconstructPath(Map<HexPosition, HexPosition> parentMap, 
                                            HexPosition start, HexPosition goal) {
        List<HexPosition> path = new ArrayList<>();
        HexPosition current = goal;
        while (current != null) {
            path.add(current);
            current = parentMap.get(current);
        }
        Collections.reverse(path);
        return path;
    }

}