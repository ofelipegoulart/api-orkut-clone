package com.orkutclone.api.validation;

import java.util.*;

public final class AllowedProfileValues {

    private AllowedProfileValues() {}

    // ── General ──

    public static final Set<String> GENDER = Set.of("masculino", "feminino", "não binário");

    public static final Set<String> RELATIONSHIP_STATUS = Set.of(
            "não há resposta", "solteiro", "casado", "namorando",
            "casamento aberto", "relacionamento aberto"
    );

    public static final Set<String> BIRTH_MONTHS = Set.of(
            "janeiro", "fevereiro", "março", "abril", "maio", "junho",
            "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
    );

    public static final Set<String> BIRTH_DAYS;
    static {
        Set<String> days = new HashSet<>();
        for (int i = 1; i <= 31; i++) days.add(String.valueOf(i));
        BIRTH_DAYS = Collections.unmodifiableSet(days);
    }

    public static final Set<String> INTERESTED_IN = Set.of(
            "amigos", "companheiros para atividades",
            "contatos profissionais", "namoro"
    );

    public static final Set<String> DATING_PREFERENCE = Set.of(
            "homens", "mulheres", "homens e mulheres"
    );

    // ── Social ──

    public static final Set<String> CHILDREN = Set.of(
            "não há resposta", "não", "sim, moram comigo",
            "sim - visitam de vez em quando", "sim - não moram comigo"
    );

    public static final Set<String> ETHNICITY = Set.of(
            "não há resposta", "afro-brasileiro (negro)", "asiático",
            "caucasiano (branco)", "índias orientais", "hispânico/latino",
            "Oriente Médio", "indígena americano", "ilhas do Oceano Pacífico",
            "multiétnico", "outra"
    );

    public static final Set<String> RELIGION = Set.of(
            "sem resposta", "Agnóstico", "Ateu", "Budista", "Cao Dai",
            "Cientologia", "Cristão/Anglicano", "Cristão/Católico",
            "Cristão/Ortodoxo", "Cristão/outro", "Cristão/Protestante",
            "Cristão/SUD", "Cristão/Espírita", "Fé Bahá'í", "Hindu",
            "Humanismo religioso", "Jaina", "Judeu", "Muçulmano",
            "Neo-Paganismo", "Rastafari", "Sikh", "Taoísta",
            "Tenho um lado espiritual independente de religiões",
            "Tenrikio", "Universalista Unitário", "Xinto", "Zoroastra", "outro"
    );

    public static final Set<String> POLITICAL_VIEW = Set.of(
            "sem resposta", "conservador de direita",
            "conservador de extrema direita", "centrista",
            "esquerda-liberal", "extrema esquerda-liberal",
            "libertário", "libertário ao extremo",
            "autoritário", "autoritário ao extremo",
            "depende", "apolítico"
    );

    public static final Set<String> SEXUAL_ORIENTATION = Set.of(
            "sem resposta", "heterossexual", "gay", "bissexual", "curioso"
    );

    public static final Set<String> HUMOR = Set.of(
            "extrovertido/extravagante", "seco/sarcástico",
            "inteligente/sagaz", "simpático", "pateta/palhaço",
            "misterioso", "grosseiro"
    );

    public static final Set<String> STYLE = Set.of(
            "alternativo", "casual", "clássico", "contemporâneo",
            "uso roupas de estilistas famosos", "minimalista",
            "natural", "esportista/amante da natureza",
            "elegante", "na moda", "urbano"
    );

    public static final Set<String> SMOKING = Set.of("não", "sim", "socialmente");

    public static final Set<String> DRINKING = Set.of("não", "sim", "socialmente");

    public static final Set<String> PETS = Set.of(
            "não gosto de animais de estimação", "tenho gatos",
            "tenho cachorros", "tenho pássaros", "tenho peixes",
            "tenho répteis", "tenho animais exóticos"
    );

    public static final Set<String> LIVING_WITH = Set.of(
            "não há resposta", "sozinho(a)", "com meus pais",
            "com meu(s) filho(s)", "com meu parceiro(a)",
            "com meu parceiro(a) e filho(s)", "com amigos",
            "com animais de estimação", "com outra(s) pessoa(s)"
    );

    // ── Personal ──

    public static final Set<String> EYE_COLOR = Set.of(
            "sem resposta", "pretos", "azuis", "castanhos",
            "cinzas", "verdes", "mel"
    );

    public static final Set<String> HAIR_COLOR = Set.of(
            "sem resposta", "castanhos avermelhado", "preto", "loiro",
            "castanho claro", "castanho escuro", "ruivo", "grisalho",
            "pouco grisalho", "careca", "muda com frequência", "outro"
    );

    public static final Set<String> BODY_TYPE = Set.of(
            "não há resposta", "magro(a)", "atlético(a)",
            "médio", "um pouco acima do peso", "gordo(a)"
    );

    public static final Set<String> APPEARANCE = Set.of(
            "não há resposta", "tipo miss/mister universo",
            "muito atraente", "atraente", "médio", "muito feio(a)"
    );

    public static final Set<String> BODY_ART = Set.of(
            "não há resposta", "tatuagem em lugar estratégico",
            "piercing na orelha", "piercing em outras partes",
            "tatuagem visível", "piercing na língua", "piercing no umbigo"
    );

    public static final Set<String> ATTRACTIONS = Set.of(
            "convicção", "luz de velas", "inteligência",
            "demonstrações públicas de afeto", "sarcasmo",
            "tatuagens", "tempestades", "piercing(s)", "dançar",
            "flertar", "cabelos compridos", "poder", "nadar nu",
            "aventura", "riqueza material"
    );

    public static final Set<String> FAVORITE_BODY_PART = Set.of(
            "não há resposta", "olhos", "cabelos", "boca", "pescoço",
            "braços", "mãos", "busto/tórax", "umbigo", "bumbum",
            "pernas", "panturrilhas", "pés", "não consta na lista"
    );

    // ── Professional ──

    public static final Set<String> EDUCATION_LEVEL = Set.of(
            "sem resposta", "ensino fundamental", "ensino médio",
            "ensino técnico", "graduação", "pós-graduação",
            "mestrado", "doutorado"
    );

    // ── Languages ──

    public static final Set<String> LANGUAGES = Set.of(
            "Africâner", "Albanês", "Alemão", "Amárico", "Árabe",
            "Armênio", "Assamês", "Azeri", "Basco", "Bengali",
            "Bielo-russo", "Birmanês", "Bósnio", "Búlgaro", "Catalão",
            "Cazaque", "Ceco", "Chinês (Cantonês)", "Chinês (Mandarim)",
            "Cingalês", "Coreano", "Croata", "Curdo", "Dinamarquês",
            "Eslovaco", "Esloveno", "Espanhol", "Esperanto", "Estoniano",
            "Filipino (Tagalo)", "Finlandês", "Francês", "Galego", "Galês",
            "Georgiano", "Grego", "Guarani", "Gujarati", "Haitiano",
            "Hauçá", "Hebraico", "Hindi", "Holandês", "Húngaro", "Igbo",
            "Indonésio", "Inglês", "Iorubá", "Irlandês", "Islandês",
            "Italiano", "Japonês", "Javanês", "Kannada", "Khmer",
            "Laosiano", "Latim", "Letão", "Lituano", "Luxemburguês",
            "Macedônio", "Malaio", "Malaiala", "Malgaxe", "Maltês",
            "Maori", "Marata", "Mongol", "Nepalês", "Norueguês", "Oriá",
            "Pachto", "Persa (Farsi)", "Polonês", "Português", "Punjabi",
            "Quirguiz", "Romeno", "Russo", "Sérvio", "Somali", "Suaíli",
            "Sueco", "Sundanês", "Tâmil", "Tailandês", "Tajique",
            "Tártaro", "Télugo", "Tibetano", "Turco", "Turcomeno",
            "Ucraniano", "Urdu", "Uzbeque", "Vietnamita", "Xhosa", "Zulu"
    );

    // ── Countries (Portuguese names) ──

    public static final Set<String> COUNTRIES = Collections.unmodifiableSet(new HashSet<>(List.of(
            "Afeganistão", "Albânia", "Alemanha", "Andorra", "Angola", "Anguilla",
            "Antilhas Holandesas", "Antártica", "Antígua e Barbuda", "Argentina",
            "Argélia", "Armênia", "Aruba", "Arábia Saudita", "Austrália",
            "Azerbaijão", "Bahamas", "Bangladesh", "Barbados", "Bareine", "Belize",
            "Benin", "Bermudas", "Bielo-Rússia", "Bolívia", "Botsuana", "Brasil",
            "Brunei", "Bulgária", "Burkina Faso", "Burundi", "Butão", "Bélgica",
            "Bósnia-Herzegóvina", "Cabo Verde", "Camarões", "Camboja", "Canadá",
            "Catar", "Cayman, Ilhas", "Cazaquistão", "Chade", "Chile", "China",
            "Chipre", "Cingapura", "Cocos (Keeling), Ilhas",
            "Colectividade de São Bartolomeu", "Colômbia", "Comores, Ilhas", "Congo",
            "Congo, República Democrática do", "Cook, Ilhas",
            "Coréia, República Popular Democrática da", "Coréia, República da",
            "Costa Rica", "Costa do Marfim", "Croácia", "Cuba", "Dinamarca",
            "Djibuti", "Dominica", "Egito", "El Salvador", "Emirados Árabes Unidos",
            "Equador", "Eritréia", "Eslováquia (República Eslovaca)", "Eslovênia",
            "Espanha", "Estados Unidos", "Estônia", "Etiópia",
            "Falkland (Ilhas Malvinas)", "Feroe, Ilhas", "Fiji", "Filipinas",
            "Finlândia", "Formosa (Taiwan)", "França", "Gabão", "Gana", "Geórgia",
            "Gibraltar", "Granada", "Groenlândia", "Grécia", "Guadalupe", "Guam",
            "Guatemala", "Guernsey", "Guiana", "Guiana Francesa",
            "Guiné-Equatorial", "Guiné", "Guiné-Bissau", "Gâmbia", "Haiti",
            "Honduras", "Hong Kong", "Hungria", "Ilha Bouvet",
            "Ilha Herad e Ilhas Macdonald", "Ilha Natal",
            "Ilhas Geórgia do Sul e Sandwich do Sul", "Indonésia",
            "Irã, República Islâmica do", "Iraque", "Irlanda", "Islândia",
            "Israel", "Itália", "Iêmen", "Jamaica", "Japão", "Jersey", "Jordânia",
            "Kiribati", "Kuwait", "Laos", "Lesoto", "Letônia", "Libéria",
            "Lichtenstein", "Lituânia", "Luxemburgo", "Líbano", "Líbia", "Macau",
            "Macedónia", "Madagascar", "Malavi", "Maldivas", "Mali", "Malta",
            "Malásia", "Man, Ilha de", "Marianas do Norte", "Marrocos",
            "Marshall, Ilhas", "Martinica", "Mauritânia", "Maurício", "Mayotte",
            "Mianmá", "Micronésia", "Moldova, República de", "Mongólia",
            "Montenegro", "Montserrat", "Moçambique", "México", "Mônaco",
            "Namíbia", "Nauru", "Nepal", "Nicarágua", "Nigéria", "Niue",
            "Norfolk, Ilha", "Noruega", "Nova Caledónia", "Nova Zelândia", "Níger",
            "Omã", "Palau", "Palestina", "Panamá", "Papua Nova Guiné",
            "Paquistão", "Paraguai", "Países Baixos", "Peru", "Pitcairn",
            "Polinésia Francesa", "Polônia", "Porto Rico", "Portugal",
            "Quirguiz, República", "Quênia", "Reino Unido",
            "República Centro-Africana", "República Dominicana", "Reunião",
            "Romênia", "Ruanda", "Rússia", "Saara Ocidental", "Saint Martin",
            "Salomão, Ilhas", "Samoa", "Samoa Americana", "San Marino",
            "Santa Helena", "Santa Lúcia",
            "Santa Sé (Cidade-Estado do Vaticano)", "Senegal", "Serra Leoa",
            "Seychelles", "Somália", "Sri Lanka", "Suazilândia", "Sudão",
            "Suriname", "Suécia", "Suíça", "Svalbard e Jan Mayen",
            "São Cristóvão e Neves, Ilhas", "São Pedro e Miquelon",
            "São Tomé e Príncipe", "São Vicente e Granadinas", "Sérvia", "Síria",
            "Tadjiquistão, República do", "Tailândia", "Tanzânia",
            "Tcheca, República", "Território Britânico do Oceano Índico",
            "Territórios Franceses do Sul", "Territórios Insulares (EUA)",
            "Timor Leste", "Togo", "Tonga", "Toquelau", "Trinidad e Tobago",
            "Tunísia", "Turcas e Caicos, Ilhas", "Turcomenistão", "Turquia",
            "Tuvalu", "Ucrânia", "Uganda", "Uruguai", "Uzbequistão", "Vanuatu",
            "Venezuela", "Vietnã", "Virgens, Ilhas (Britânicas)",
            "Virgens, Ilhas (E.U.A.)", "Wallis e Futuna, Ilhas", "Zimbábue",
            "Zâmbia", "África do Sul", "Áustria", "Índia"
    )));

    // ── Validation helpers ──

    public static void validateOption(String value, Set<String> allowed, String fieldName) {
        if (value != null && !allowed.contains(value)) {
            throw new IllegalArgumentException("Invalid value for " + fieldName + ": " + value);
        }
    }

    public static void validateOptionList(List<String> values, Set<String> allowed, String fieldName) {
        if (values != null) {
            for (String value : values) {
                if (!allowed.contains(value)) {
                    throw new IllegalArgumentException("Invalid value for " + fieldName + ": " + value);
                }
            }
        }
    }
}
