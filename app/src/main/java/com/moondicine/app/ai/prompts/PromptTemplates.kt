package com.moondicine.app.ai.prompts

object PromptTemplates {

    fun systemPrompt(): String = """
        Você é um especialista em educação médica para provas de residência médica no Brasil.
        Responda sempre e exclusivamente em português do Brasil, incluindo explicações, campos de texto,
        classificações e mensagens. Nunca responda em inglês, salvo quando o texto original exigir a preservação.
        Você conhece as especialidades cobradas em provas como ENAM, ANCAR e VINCI.
        Forneça informações médicas precisas e baseadas em evidências.
        Quando solicitado a analisar ou gerar questões, responda sempre em JSON válido.
    """.trimIndent()

    fun parseQuestionsPrompt(textChunk: String): String = """
        Analise o texto a seguir, extraído de um PDF de prova de residência médica.
        Identifique e extraia cada questão, suas alternativas e a resposta correta, quando disponível.

        Para cada questão, retorne um objeto JSON com:
        - questionNumber: número da questão (inteiro)
        - questionText: texto completo da questão (string)
        - options: array de objetos com "letter" (string: "A", "B", "C", "D", "E") e "text" (string)
        - correctAnswer: letra da alternativa correta, quando disponível, ou null
        - specialty: classifique em exatamente uma destas cinco especialidades: "Clínica Médica", "Cirurgia Geral", "Pediatria", "Ginecologia e Obstetrícia", "Medicina Preventiva"
        - subTopic: subtema específico dentro da especialidade (string)
        - difficulty: dificuldade estimada de 1 a 5, em que 1 é mais fácil e 5 é mais difícil (inteiro)

        Retorne um objeto JSON com um array "questions" contendo todas as questões extraídas.
        Exemplo:
        {"questions": [{"questionNumber": 1, "questionText": "...", "options": [{"letter": "A", "text": "..."}], "correctAnswer": "C", "specialty": "Clínica Médica", "subTopic": "Cardiologia", "difficulty": 3}]}

        Text to analyze:
        ---
        $textChunk
        ---
    """.trimIndent()

    fun generateExplanationPrompt(
        questionText: String,
        options: Map<String, String>,
        correctAnswer: String,
        userAnswer: String?
    ): String {
        val optionsText = options.entries.joinToString("\n") { (letter, text) -> "$letter) $text" }
        val userAnswerText = userAnswer?.let { "\nResposta do usuário: $it" } ?: ""

        return """
            Você é um professor especialista em educação médica para provas de residência médica no Brasil.
            Sua tarefa é fornecer uma explicação EXTREMAMENTE DETALHADA e PROFUNDA sobre a questão abaixo.

            Questão: $questionText

            Alternativas:
            $optionsText

            Resposta correta: $correctAnswer) ${options[correctAnswer] ?: ""}
            $userAnswerText

            Retorne um objeto JSON com:
            - "correctReasoning": explicação MUITO detalhada e aprofundada (mínimo 5-8 frases) do motivo pelo qual a resposta correta está certa. Inclua:
              * O raciocínio clínico passo a passo
              * A fisiopatologia ou mecanismo envolvido
              * Dados estatísticos ou evidências científicas quando relevante
              * Por que essa é a melhor escolha entre todas as alternativas
              * Conexão com a prática clínica real
            - "wrongReasoning": objeto JSON associando CADA alternativa errada (A, B, C, D, E - exceto a correta) a uma explicação detalhada de 2-4 frases sobre:
              * Por que aquela alternativa está errada
              * O erro conceitual mais comum associado a ela
              * Qual seria a situação em que aquela alternativa poderia estar correta (se aplicável)
            - "highYieldPoints": array com 3 a 5 pontos essenciais e de alto rendimento para prova, cada um com 1-2 frases explicativas
            - "relatedTopics": array com 3 a 4 temas relacionados para estudo aprofundado

            Formato: responda APENAS com o objeto JSON válido, sem texto adicional antes ou depois.
            Idioma: exclusivamente português do Brasil.
        """.trimIndent()
    }

    fun baselineAssessmentPrompt(): String = """
        Gere 20 questões de múltipla escolha em português do Brasil para uma avaliação inicial de residência médica.
        Cubra estas áreas com distribuição aproximadamente equilibrada:
        - 3-4 questões de Clínica Médica
        - 2-3 questões de Pediatria
        - 2-3 questões de Ginecologia e Obstetrícia
        - 2-3 questões de Cirurgia Geral
        - 2-3 questões de Medicina Preventiva
        - 1-2 questões de Psiquiatria
        - 1-2 questões de Medicina de Família
        - 1-2 questões de outras especialidades

        Cada questão deve ter 5 alternativas (A-E) e uma única resposta correta.
        Varie a dificuldade entre fácil, média e difícil.

        Retorne JSON: {"questions": [...]}
    """.trimIndent()
}
