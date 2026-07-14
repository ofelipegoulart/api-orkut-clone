package com.orkutclone.api.model.enums;

/**
 * Fixed set of community categories offered by the creation form.
 * The constant name is what travels over the API; {@link #getLabel()} carries the
 * Portuguese text the form renders in its dropdown.
 */
public enum CommunityCategory {

    STUDENTS_AND_SCHOOLS("Alunos e Escolas"),
    ANIMALS("Animais: de estimação ou não"),
    ARTS_AND_ENTERTAINMENT("Artes e Entretenimento"),
    ACTIVITIES("Atividades"),
    AUTOMOTIVE("Automotivo"),
    CITIES_AND_NEIGHBORHOODS("Cidades e Bairros"),
    COMPUTERS_AND_INTERNET("Computadores e Internet"),
    FOOD_DRINKS_AND_WINE("Culinária, Bebidas e Vinhos"),
    CULTURES_AND_COMMUNITY("Culturas e Comunidade"),
    COMPANY("Empresa"),
    SCHOOLS_AND_COURSES("Escolas e Cursos"),
    SPORTS_AND_LEISURE("Esportes e Lazer"),
    FAMILY_AND_HOME("Família e Lar"),
    GAY_LESBIAN_AND_BI("Gays, Lésbicas e Bi"),
    GOVERNMENT_AND_POLITICS("Governo e Política"),
    HISTORY_AND_SCIENCES("História e Ciências"),
    HOBBIES_AND_CRAFTS("Hobbies e Trabalhos Manuais"),
    GAMES("Jogos"),
    FASHION_AND_BEAUTY("Moda e Beleza"),
    MUSIC("Música"),
    BUSINESS("Negócios"),
    COUNTRIES_AND_REGIONS("Países e Regiões"),
    PEOPLE("Pessoas"),
    RELIGIONS_AND_BELIEFS("Religiões e Crenças"),
    ROMANCE_AND_RELATIONSHIPS("Romances e Relacionamentos"),
    HEALTH_WELLNESS_AND_FITNESS("Saúde, Bem-estar e Fitness"),
    TRAVEL("Viagens"),
    OTHERS("Outros");

    private final String label;

    CommunityCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
