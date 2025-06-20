package com.atraparalagato.impl.strategy;

import com.atraparalagato.base.model.GameBoard;
import com.atraparalagato.base.strategy.CatMovementStrategy;
import com.atraparalagato.impl.model.HexPosition;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Implementación esqueleto de estrategia de movimiento usando algoritmo A*.
 * 
 * Los estudiantes deben completar los métodos marcados con TODO.
 * 
 * Conceptos a implementar:
 * - Algoritmos: A* pathfinding
 * - Programación Funcional: Function, Predicate
 * - Estructuras de Datos: PriorityQueue, Map, Set
 */
public class AStarCatMovement extends CatMovementStrategy<HexPosition> {
    
    public AStarCatMovement(GameBoard<HexPosition> board) {
        super(board);
    }
    
    @Override
    protected List<HexPosition> getPossibleMoves(HexPosition currentPosition) {
        // Obtener posiciones adyacentes que no estén bloqueadas
        return board.getAdjacentPositions(currentPosition).stream()
                .filter(pos -> !board.isBlocked(pos))
                .toList();
    }

    @Override
    protected Optional<HexPosition> selectBestMove(List<HexPosition> possibleMoves,
                                                  HexPosition currentPosition,
                                                  HexPosition targetPosition) {
        // Selecciona el movimiento cuyo camino A* al objetivo sea más corto
        double minCost = Double.POSITIVE_INFINITY;
        HexPosition bestMove = null;
        for (HexPosition move : possibleMoves) {
            List<HexPosition> path = getFullPath(move, targetPosition);
            if (!path.isEmpty() && path.size() < minCost) {
                minCost = path.size();
                bestMove = move;
            }
        }
        return Optional.ofNullable(bestMove);
    }

    @Override
    protected Function<HexPosition, Double> getHeuristicFunction(HexPosition targetPosition) {
        // Distancia hexagonal (admisible)
        return position -> position.distanceTo(targetPosition);
    }

    @Override
    protected Predicate<HexPosition> getGoalPredicate() {
        // El objetivo es llegar al borde del tablero
        return position -> {
            int boardSize = board.getSize();
            return Math.abs(position.getQ()) == boardSize ||
                   Math.abs(position.getR()) == boardSize ||
                   Math.abs(position.getS()) == boardSize;
        };
    }

    @Override
    protected double getMoveCost(HexPosition from, HexPosition to) {
        // Costo uniforme para movimientos adyacentes
        return 1.0;
    }

    @Override
    public boolean hasPathToGoal(HexPosition currentPosition) {
        // BFS simple para verificar si hay camino al borde
        Set<HexPosition> visited = new HashSet<>();
        Queue<HexPosition> queue = new LinkedList<>();
        queue.offer(currentPosition);
        visited.add(currentPosition);

        Predicate<HexPosition> goalPredicate = getGoalPredicate();

        while (!queue.isEmpty()) {
            HexPosition current = queue.poll();
            if (goalPredicate.test(current)) {
                return true;
            }
            for (HexPosition neighbor : board.getAdjacentPositions(current)) {
                if (!visited.contains(neighbor) && !board.isBlocked(neighbor)) {
                    visited.add(neighbor);
                    queue.offer(neighbor);
                }
            }
        }
        return false;
    }

    @Override
    public List<HexPosition> getFullPath(HexPosition currentPosition, HexPosition targetPosition) {
        // Implementación básica de A*
        Predicate<HexPosition> goalPredicate = getGoalPredicate();
        Function<HexPosition, Double> heuristic = getHeuristicFunction(targetPosition);

        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<HexPosition, Double> gScore = new HashMap<>();
        Set<HexPosition> closedSet = new HashSet<>();

        AStarNode startNode = new AStarNode(currentPosition, 0.0, heuristic.apply(currentPosition), null);
        openSet.add(startNode);
        gScore.put(currentPosition, 0.0);

        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();

            if (goalPredicate.test(current.position) || current.position.equals(targetPosition)) {
                return reconstructPath(current);
            }

            closedSet.add(current.position);

            for (HexPosition neighbor : board.getAdjacentPositions(current.position)) {
                if (board.isBlocked(neighbor) || closedSet.contains(neighbor)) continue;

                double tentativeG = current.gScore + getMoveCost(current.position, neighbor);
                if (tentativeG < gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    gScore.put(neighbor, tentativeG);
                    double fScore = tentativeG + heuristic.apply(neighbor);
                    openSet.add(new AStarNode(neighbor, tentativeG, fScore, current));
                }
            }
        }
        return Collections.emptyList();
    }

    // Clase auxiliar para nodos del algoritmo A*
    private static class AStarNode {
        public final HexPosition position;
        public final double gScore; // Costo desde inicio
        public final double fScore; // gScore + heurística
        public final AStarNode parent;
        
        public AStarNode(HexPosition position, double gScore, double fScore, AStarNode parent) {
            this.position = position;
            this.gScore = gScore;
            this.fScore = fScore;
            this.parent = parent;
        }
    }
    
    // Método auxiliar para reconstruir el camino
    private List<HexPosition> reconstructPath(AStarNode goalNode) {
        List<HexPosition> path = new ArrayList<>();
        AStarNode current = goalNode;
        while (current != null) {
            path.add(current.position);
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }
    
    // Hook methods - los estudiantes pueden override para debugging
    @Override
    protected void beforeMovementCalculation(HexPosition currentPosition) {
        // TODO: Opcional - logging, métricas, etc.
        super.beforeMovementCalculation(currentPosition);
    }
    
    @Override
    protected void afterMovementCalculation(Optional<HexPosition> selectedMove) {
        // TODO: Opcional - logging, métricas, etc.
        super.afterMovementCalculation(selectedMove);
    }
}