package fr.perso.zoe.dto;

import java.util.List;

public record ImportResultDto(int recharges, int tarifs, int releves, boolean parametres, List<String> avertissements) {}
