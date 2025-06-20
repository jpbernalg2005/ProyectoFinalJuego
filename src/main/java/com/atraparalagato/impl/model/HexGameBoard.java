package com.atraparalagato.impl.model;

import com.atraparalagato.base.model.GameBoard;


import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Implementación esqueleto de GameBoard para tableros hexagonales.
 * 
 * Los estudiantes deben completar los métodos marcados con TODO.
 * 
 * Conceptos a implementar:
 * - Modularización: Separación de lógica de tablero hexagonal
 * - OOP: Herencia y polimorfismo
 * - Programación Funcional: Uso de Predicate y streams
 */
public class HexGameBoard extends GameBoard<HexPosition> {
    
    public HexGameBoard(int size) {
        super(size);
    }
    
    @Override
    protected Set<HexPosition> initializeBlockedPositions() {
        return new HashSet<>();
    }
    
    @Override
    protected boolean isPositionInBounds(HexPosition position) {
        return Math.abs(position.getQ()) <= size && 
                Math.abs(position.getR()) <= size && 
                Math.abs(position.getS()) <= size;
    }
    
    @Override
    protected boolean isValidMove(HexPosition position) {
        return isPositionInBounds(position) && !isAtBorder(position) && !isBlocked(position);
    }
    
    @Override
    protected void executeMove(HexPosition position) {
       blockedPositions.add(position);
    }
    
    @Override
    public List<HexPosition> getPositionsWhere(Predicate<HexPosition> condition) {
            return getAllPossiblePositions().stream()
                .filter(condition)
                .collect(Collectors.toList());
    
    }
    
    @Override
    public List<HexPosition> getAdjacentPositions(HexPosition position) {
         // Direcciones hexagonales: las 6 direcciones posibles
        HexPosition[] directions = {
            new HexPosition(1, 0),   // Este
            new HexPosition(1, -1),  // Noreste
            new HexPosition(0, -1),  // Noroeste
            new HexPosition(-1, 0),  // Oeste
            new HexPosition(-1, 1),  // Suroeste
            new HexPosition(0, 1)    // Sureste
        };
        
        return Arrays.stream(directions)
                .map(dir -> (HexPosition) position.add(dir))
                .filter(this::isPositionInBounds) // Incluye posiciones del borde
                .filter(pos -> !isBlocked(pos))   // Excluye posiciones bloqueadas
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean isBlocked(HexPosition position) {
         return blockedPositions.contains(position);
         
    }
    public boolean isAtBorder(HexPosition position) {
        return Math.abs(position.getQ()) == size ||
               Math.abs(position.getR()) == size ||
               Math.abs(position.getS()) == size;
    }
    
    // Método auxiliar que los estudiantes pueden implementar
    private List<HexPosition> getAllPossiblePositions() {
        List<HexPosition> positions = new ArrayList<>();
        
        // Generar todas las posiciones dentro del tablero (excluyendo el borde para jugabilidad)
        for (int q = -size + 1; q < size; q++) {
            for (int r = -size + 1; r < size; r++) {
                HexPosition pos = new HexPosition(q, r);
                // Solo incluir posiciones que no están en el borde (donde el jugador puede jugar)
                if (isPositionInBounds(pos) && !isAtBorder(pos)) {
                    positions.add(pos);
                }
            }
        }
        
        return positions;
    }
   // Método adicional útil para debugging
    public void printBoard() {
        System.out.println("Estado del tablero (tamaño: " + size + "):");
        System.out.println("Posiciones bloqueadas: " + blockedPositions.size());
        
        // Imprimir algunas estadísticas básicas
        long totalPositions = getAllPossiblePositions().size();
        System.out.println("Total de posiciones: " + totalPositions);
        System.out.println("Posiciones libres: " + (totalPositions - blockedPositions.size()));
    }
    
    // Método para obtener estadísticas del tablero
    public Map<String, Object> getBoardStatistics() {
        Map<String, Object> stats = new HashMap<>();
        List<HexPosition> allPositions = getAllPossiblePositions();
        
        stats.put("boardSize", size);
        stats.put("totalPositions", allPositions.size());
        stats.put("blockedPositions", blockedPositions.size());
        stats.put("freePositions", allPositions.size() - blockedPositions.size());
        stats.put("blockagePercentage", 
                  (double) blockedPositions.size() / allPositions.size() * 100);
        
        return stats;
    }
} 