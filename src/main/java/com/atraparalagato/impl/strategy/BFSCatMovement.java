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
        // Devuelve posiciones adyacentes no bloqueadas
        List<HexPosition> adjacents = board.getAdjacentPositions(currentPosition);
        List<HexPosition> valid = new ArrayList<>();
        for (HexPosition pos : adjacents) {
            if (!board.isBlocked(pos)) {
                valid.add(pos);
            }
        }
        return valid;
    }
    
    @Override
    protected Optional<HexPosition> selectBestMove(List<HexPosition> possibleMoves, 
                                                  HexPosition currentPosition, 
                                                  HexPosition targetPosition) {
        // Elige el movimiento cuyo camino BFS al objetivo sea más corto
        HexPosition bestMove = null;
        int minLength = Integer.MAX_VALUE;
        for (HexPosition move : possibleMoves) {
            List<HexPosition> path = bfsToGoal(move).orElse(Collections.emptyList());
            if (!path.isEmpty() && path.size() < minLength) {
                minLength = path.size();
                bestMove = move;
            }
        }
        return Optional.ofNullable(bestMove);
    }
    
    @Override
    protected Function<HexPosition, Double> getHeuristicFunction(HexPosition targetPosition) {
        // BFS no usa heurística, pero devolvemos distancia para desempate si se requiere
        return pos -> (double) pos.distanceTo(targetPosition);
    }
    
    @Override
    protected Predicate<HexPosition> getGoalPredicate() {
        // El objetivo es llegar al borde del tablero
        int size = board.getSize();
        return pos -> Math.abs(pos.getQ()) == size ||
                      Math.abs(pos.getR()) == size ||
                      Math.abs(pos.getS()) == size;
    }
    
    @Override
    protected double getMoveCost(HexPosition from, HexPosition to) {
        // Costo uniforme para BFS
        return 1.0;
    }
    
    @Override
    public boolean hasPathToGoal(HexPosition currentPosition) {
        // Usa BFS para verificar si hay camino al borde
        return bfsToGoal(currentPosition).isPresent();
    }
    
    @Override
    public List<HexPosition> getFullPath(HexPosition currentPosition, HexPosition targetPosition) {
        // Usa BFS para encontrar el camino más corto al objetivo
        return bfsToGoal(currentPosition).orElse(Collections.emptyList());
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