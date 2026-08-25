# MoonDice - Medical Residency Exam Practice App

## Comprehensive App Briefing

---

## 1. App Overview

**MoonDice** is a mobile study application designed for medical students preparing for residency entrance exams (ENAM, ANCAR, VINCI, and similar exams across Latin America and beyond). The app allows users to upload exam PDFs (with or without separate answer keys), uses Cohere AI to parse, categorize, and store questions in a local database, and provides an interactive study experience with AI-powered explanations.

### Core Value Proposition
- Transform static PDF exams into an intelligent, interactive study tool
- AI-generated explanations for every answer choice
- Personalized analytics on performance by medical specialty
- spaced-repetition-inspired review of weak areas

---

## 2. Target Audience

| Segment | Description |
|---------|-------------|
| **Primary** | Final-year medical students preparing for residency entrance exams |
| **Secondary** | Medical graduates re-attempting residency exams |
| **Tertiary** | Medical educators who want to digitize exam banks |

---

## 3. Features Breakdown

### 3.1 Onboarding & User Profile

#### Onboarding Flow (First Launch)
1. **Welcome Screen** — App introduction with key features
2. **Experience Level** — User selects their experience:
   - First-time exam taker
   - Retaking (1st attempt)
   - Retaking (2+ attempts)
3. **Target Specialty Selection** — User selects which residency they're targeting (helps prioritize relevant areas):
   - Clínica Médica
   - Cirurgia Geral
   - Pediatria
   - Ginecologia e Obstetrícia
   - Ortopedia e Traumatologia
   - Psiquiatria
   - Anestesiologia
   - Neurologia
   - Medicina Preventiva / Saúde Coletiva
   - Cardiologia
   - Dermatologia
   - Oftalmologia
   - Otorrinolaringologia
   - Urologia
   - Outro / Ainda não decidido
4. **Baseline Assessment** — A 20-question diagnostic quiz drawn from mixed specialties to gauge the user's current level. Results feed into the initial difficulty calibration.

#### User Profile
- Name (optional)
- Target specialty
- Experience level
- Join date
- Total questions answered
- Current streak (days studied)
- Total study time

### 3.2 PDF Upload & Processing

#### Upload Screen
- **Single PDF Mode**: One file containing both questions and answers
- **Dual PDF Mode**: Two files — one with questions, one with answer keys
- File picker using Android Storage Access Framework
- Supported formats: PDF only
- Max file size: 50MB per file
- Progress indicator during upload and processing

#### PDF Text Extraction
- Uses AndroidPdfRenderer or Apache PDFBox for Android to extract raw text
- Handles multi-column layouts, tables, images with alt-text
- Preserves question numbering and answer letter formatting
- Falls back to OCR (if available) for scanned PDFs

#### AI Processing Pipeline (Cohere API)
1. **Text Chunking**: Split extracted text into manageable chunks (within Cohere context limits)
2. **Question Identification**: AI identifies question boundaries, options (A-E), and correct answers
3. **Subject Classification**: Each question is tagged with:
   - **Primary specialty** (e.g., Cardiologia)
   - **Sub-topic** (e.g., Arritmias)
   - **Difficulty estimate** (based on historical pass rates for similar questions)
4. **Answer Key Matching**: If two PDFs are uploaded, cross-reference questions with answers
5. **Storage**: Parsed questions saved to local Room database

#### Processing States
- `UPLOADING` → `EXTRACTING_TEXT` → `ANALYZING_WITH_AI` → `PARSING_QUESTIONS` → `CLASSIFYING` → `STORING` → `COMPLETE`
- Error states with retry capability

### 3.3 Question Database Schema

#### Question Entity
```
Question {
  id: Long (auto-generated)
  examSource: String          // e.g., "ENAM 2024", "ANCAR 2023"
  questionNumber: Int         // Number within the exam
  questionText: String        // Full question text
  specialty: String           // Primary medical specialty
  subTopic: String            // Specific sub-topic
  difficulty: Int             // 1-5 scale
  imageUrl: String?           // Optional image reference
  createdAt: Long
  updatedAt: Long
}
```

#### AnswerOption Entity
```
AnswerOption {
  id: Long (auto-generated)
  questionId: Long (FK)
  optionLetter: String       // "A", "B", "C", "D", "E"
  optionText: String
  isCorrect: Boolean
}
```

#### UserAnswer Entity
```
UserAnswer {
  id: Long (auto-generated)
  questionId: Long (FK)
  selectedOptionId: Long (FK)
  isCorrect: Boolean
  timeSpentSeconds: Int
  answeredAt: Long
  isFlagged: Boolean         // User flagged for review
}
```

#### QuestionNote Entity
```
QuestionNote {
  id: Long (auto-generated)
  questionId: Long (FK)
  noteText: String
  createdAt: Long
  updatedAt: Long
}
```

#### AIExplanation Entity
```
AIExplanation {
  id: Long (auto-generated)
  questionId: Long (FK)
  explanationText: String    // Full AI explanation
  correctReasoning: String   // Why correct answer is correct
  wrongReasoning: String     // JSON map of option -> why it's wrong
  cachedAt: Long
}
```

#### UserStats Entity
```
UserStats {
  id: Long (auto-generated)
  specialty: String
  totalAnswered: Int
  totalCorrect: Int
  averageTimeSeconds: Float
  lastStudiedAt: Long
  weakestSubTopics: String   // JSON array
}
```

### 3.4 Quiz Mode

#### Quiz Types
1. **Quick Quiz** — 10 random questions from all specialties
2. **Specialty Quiz** — Filtered by one or more specialties
3. **Weak Areas Quiz** — Focuses on questions the user gets wrong most often
4. **Exam Simulation** — Full-length timed exam (40-60 questions, configurable)
5. **Daily Challenge** — 5 curated questions daily, mixed difficulty

#### Quiz Flow
1. User selects quiz type and parameters (optional filters)
2. Questions presented one at a time in card format
3. User selects an answer (A-E)
4. **Immediate feedback**:
   - Highlights correct answer in green
   - Highlights selected wrong answer in red
   - Shows AI-generated explanation (expandable)
   - Shows why each wrong answer is incorrect
5. Navigation: Previous / Next / Flag for Review / End Quiz
6. **Timer**: Optional countdown for exam simulation mode
7. **Progress bar**: Shows position in quiz (e.g., "Question 5 of 20")

#### Question Card Layout
```
┌─────────────────────────────┐
│ [Specialty Tag]  [Difficulty]│
│ Question #12 — ENAM 2024   │
│                              │
│ A 45-year-old male presents │
│ with chest pain...           │
│                              │
│ ○ A) Acute MI               │
│ ○ B) Pulmonary embolism     │
│ ○ C) Aortic dissection      │
│ ○ D) Pneumothorax           │
│ ○ E) GERD                   │
│                              │
│ ─────────────────────────── │
│ 💬 Notes  🚩 Flag  ⏱ Timer │
└─────────────────────────────┘
```

#### Post-Answer Layout
```
┌─────────────────────────────┐
│ ✅ Correct! / ❌ Incorrect  │
│                              │
│ Correct Answer: C)           │
│ Aortic dissection            │
│                              │
│ 📖 Explanation              │
│ ┌─────────────────────────┐ │
│ │ Aortic dissection is... │ │
│ │ The key clinical finding│ │
│ │ is...                   │ │
│ └─────────────────────────┘ │
│                              │
│ ❌ Why A is wrong:          │
│   Acute MI typically...      │
│ ❌ Why B is wrong:          │
│   Pulmonary embolism...     │
│ ❌ Why D is wrong:          │
│   Pneumothorax would...     │
│ ❌ Why E is wrong:          │
│   GERD pain is typically... │
│                              │
│ 💬 Add Note                 │
└─────────────────────────────┘
```

### 3.5 AI Explanations (Cohere API)

#### Explanation Generation
When a user answers a question, the app sends a prompt to Cohere with:
- The question text
- All answer options
- The correct answer
- The user's selected answer (if wrong)

The AI generates:
1. **Correct answer explanation**: Why this is the right answer with clinical reasoning
2. **Wrong answer explanations**: Why each other option is incorrect
3. **High-yield teaching points**: Key concepts for exam preparation
4. **Related topics**: Suggestions for further study

#### Caching Strategy
- AI explanations are cached locally after first generation
- Cache invalidation: never (medical knowledge is stable)
- Lazy generation: explanations generated on-demand for questions not yet answered
- Pre-generation option: background generation for upcoming quiz questions

#### Cohere API Configuration
- Model: `command-r-plus` (for complex medical reasoning)
- Temperature: 0.3 (low creativity, high accuracy)
- Max tokens: 2000 per explanation
- System prompt: Medical education specialist persona with emphasis on evidence-based reasoning

### 3.6 Subject Classification System

#### Primary Medical Specialties (Residency Exam Areas)

Based on analysis of major medical residency entrance exams (ENAM Brazil, ANCAR, USMLE equivalent areas), here are the primary specialty domains and their typical exam weight:

| # | Specialty | Typical Exam Weight | Key Sub-topics |
|---|-----------|-------------------|----------------|
| 1 | **Clínica Médica (Internal Medicine)** | 25-35% | Cardiovascular, Pulmonary, GI/Hepatology, Nephrology, Endocrinology, Hematology, Infectious Disease, Rheumatology, Neurology, Dermatology, Emergency Medicine |
| 2 | **Pediatria (Pediatrics)** | 12-18% | Neonatology, Growth & Development, Vaccination, Pediatric Cardiology, Pediatric Pulmonology, Pediatric GI, Pediatric Neurology, Pediatric Emergency, Nutritional deficiencies |
| 3 | **Ginecologia e Obstetrícia (OB/GYN)** | 12-18% | Prenatal Care, Labor & Delivery, Contraception, Menopause, Gynecological Oncology, STIs, Infertility, High-risk Pregnancy |
| 4 | **Cirurgia Geral (General Surgery)** | 12-18% | Acute Abdomen, Trauma, Hernias, Abdominal Wall, Colorectal, Vascular, Breast, Thyroid, Transplant, Oncological Surgery |
| 5 | **Medicina Preventiva e Social** | 8-12% | Epidemiology, Biostatistics, Public Health Policies, SUS (Brazilian Health System), Vaccination Programs, Environmental Health, Evidence-Based Medicine |
| 6 | **Psiquiatria (Psychiatry)** | 5-8% | Mood Disorders, Psychotic Disorders, Anxiety Disorders, Substance Use, Personality Disorders, Child Psychiatry, Geriatric Psychiatry |
| 7 | **Médicina de Família (Family Medicine)** | 5-8% | Primary Care, Chronic Disease Management, Prevention & Health Promotion, Women's Health, Child Health, Elderly Care |
| 8 | **Ortopedia e Traumatologia** | 3-6% | Fractures, Joint Injuries, Spine, Pediatric Orthopedics, Arthroplasty, Sports Medicine |
| 9 | **Anestesiologia** | 2-4% | Anesthesia Techniques, Pain Management, Airway Management, Perioperative Care |
| 10 | **Neurologia** | 3-5% | Stroke, Epilepsy, Demyelinating Diseases, Movement Disorders, Headache, Neuromuscular |
| 11 | **Urologia** | 2-4% | BPH, Renal Stones, Urological Oncology, Infections, Trauma |
| 12 | **Oftalmologia** | 1-3% | Glaucoma, Cataract, Retinal Diseases, Emergency Eye Care |
| 13 | **Dermatologia** | 2-4% | Dermatoses, Skin Cancer, STIs (cutaneous), Autoimmune Skin Diseases |
| 14 | **Otorrinolaringologia** | 1-3% | ENT Emergencies, Hearing Loss, Sinusitis, Head & Neck |
| 15 | **Radiologia** | 1-2% | Imaging Interpretation, Radiation Safety |

#### Sub-topic Classification (Detailed)

**Clínica Médica Sub-topics:**
- Cardiovascular: Hypertension, Heart Failure, ACS/MI, Arrhythmias, Valvular Disease, Cardiomyopathies, Vascular Disease
- Pulmonary: Asthma, COPD, Pneumonia, PE, Lung Cancer, TB, Interstitial Disease
- GI/Hepatology: GERD, Peptic Ulcer, IBD, Liver Cirrhosis, Hepatitis, Pancreatitis, GI Bleeding
- Nephrology: AKI, CKD, Glomerulonephritis, Electrolyte Disorders, Acid-Base, Dialysis
- Endocrinology: Diabetes, Thyroid, Adrenal, Pituitary, Bone/Metabolic, Obesity
- Hematology: Anemias, Leukemias, Lymphomas, Coagulopathies, Transfusion Medicine
- Infectious: HIV/AIDS, Sepsis, UTI, Meningitis, Dengue/Zika, Parasitosis, Tropical Medicine
- Rheumatology: RA, SLE, Gout, Vasculitis, Spondyloarthropathies
- Neurology: Stroke, Epilepsy, Headache, Dementia, Neuropathies, MS

**Pediatria Sub-topics:**
- Neonatology: Neonatal Resuscitation, RDS, Sepsis Neonatal, Hyperbilirubinemia, Congenital Anomalies
- Growth & Development: Milestones, Failure to Thrive, Obesity
- Vaccination: Schedule, Contraindications, Adverse Events, Special Situations
- Pediatric Emergency: Status Epilepticus, Bronchiolitis, Croup, Dehydration, Poisoning

**Ginecologia e Obstetrícia Sub-topics:**
- Prenatal: Screening, Complications (Pre-eclampsia, GDM, Placenta), Fetal Surveillance
- Labor: Normal Labor, Dystocia, Fetal Distress, Operative Delivery
- Contraception: Methods, Emergency Contraception, Counseling
- Oncology: Cervical, Ovarian, Endometrial, Breast in Pregnancy

### 3.7 Notes & Comments System

- Users can add text notes to any question
- Notes are visible during review and when revisiting questions
- Search within notes
- Export notes (future feature)

### 3.8 Statistics & Analytics Dashboard

#### Dashboard Metrics
- **Overall Accuracy**: % correct across all questions
- **Questions by Specialty**: Accuracy breakdown per specialty
- **Progress Over Time**: Chart showing improvement
- **Time Analysis**: Average time per question, time trends
- **Streak Counter**: Days of consecutive practice
- **Ranking**: Estimated percentile vs. historical exam averages

#### Weak Areas Analysis
- Automatically identifies specialties and sub-topics where user struggles
- Generates "Study Plan" recommendations
- Tracks improvement in weak areas over time

#### Charts & Visualizations
- Radar chart: Accuracy by specialty
- Line chart: Accuracy over time
- Bar chart: Questions answered per day/week
- Heat map: Study activity calendar (GitHub-style)

### 3.9 Review Mode

#### Wrong Question Review
- Dedicated screen showing all questions answered incorrectly
- Sorted by: frequency of errors, specialty, most recent
- Filter by specialty, difficulty, date range
- Re-attempt wrong questions option

#### Flagged Questions Review
- Questions manually flagged by user
- Separate from wrong answers — for questions user wants to revisit

#### spaced Repetition (Simplified)
- Questions answered incorrectly are presented more frequently
- Algorithm weights: recent errors > old errors, hard questions > easy questions
- Configurable intervals for review recommendations

---

## 4. Technical Architecture

### 4.1 Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI Framework | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| Local Database | Room |
| Dependency Injection | Hilt |
| Navigation | Navigation Compose |
| PDF Extraction | Apache PDFBox for Android |
| HTTP Client | Retrofit + OkHttp |
| AI Service | Cohere API (command-r-plus) |
| Coroutines | Kotlin Coroutines + Flow |
| Image Loading | Coil |
| Charts | Vico or custom Compose Canvas |
| Build System | Gradle with Kotlin DSL |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

### 4.2 Project Structure

```
com.moondicine.app/
├── MoonDiceApp.kt              // Application class (Hilt)
├── MainActivity.kt
├── data/
│   ├── database/
│   │   ├── AppDatabase.kt
│   │   ├── converters/         // Type converters
│   │   ├── dao/
│   │   │   ├── QuestionDao.kt
│   │   │   ├── UserAnswerDao.kt
│   │   │   ├── NoteDao.kt
│   │   │   ├── StatsDao.kt
│   │   │   └── ExplanationDao.kt
│   │   └── entity/
│   │       ├── QuestionEntity.kt
│   │       ├── AnswerOptionEntity.kt
│   │       ├── UserAnswerEntity.kt
│   │       ├── QuestionNoteEntity.kt
│   │       ├── AIExplanationEntity.kt
│   │       └── UserStatsEntity.kt
│   ├── repository/
│   │   ├── QuestionRepository.kt
│   │   ├── UserProgressRepository.kt
│   │   └── SettingsRepository.kt
│   └── pdf/
│       └── PdfTextExtractor.kt
├── ai/
│   ├── CohereApi.kt            // Retrofit interface
│   ├── CohereService.kt        // Business logic wrapper
│   ├── models/                 // Request/Response DTOs
│   └── prompts/                // Prompt templates
├── domain/
│   ├── model/                  // Domain models
│   └── usecase/                // Use cases
├── di/                         // Hilt modules
│   ├── DatabaseModule.kt
│   ├── NetworkModule.kt
│   └── AppModule.kt
└── ui/
    ├── theme/
    │   ├── Theme.kt
    │   ├── Color.kt
    │   └── Type.kt
    ├── navigation/
    │   └── AppNavigation.kt
    ├── screens/
    │   ├── onboarding/
    │   ├── upload/
    │   ├── quiz/
    │   ├── review/
    │   ├── stats/
    │   ├── notes/
    │   └── home/
    └── components/
        ├── QuestionCard.kt
        ├── AnswerOptionButton.kt
        ├── ExplanationSheet.kt
        ├── SpecialtyChip.kt
        ├── ProgressRing.kt
        └── Charts/
```

### 4.3 Data Flow

```
PDF Upload
    → PdfTextExtractor.extractText()
    → CohereService.parseQuestions(text)
    → List<QuestionDomainModel>
    → QuestionRepository.insertAll()
    → Room Database

Quiz Mode
    → QuestionRepository.getQuestions(filter)
    → User selects answer
    → UserAnswerRepository.save()
    → CohereService.generateExplanation() (if not cached)
    → AIExplanationRepository.cache()
    → StatsRepository.updateStats()
```

### 4.4 Cohere API Integration

#### API Endpoint
```
POST https://api.cohere.ai/v1/chat
```

#### Headers
```
Authorization: Bearer <API_KEY>
Content-Type: application/json
```

#### Key Prompts

**Question Parsing Prompt:**
```
You are a medical education expert. Analyze the following text extracted from a medical 
residency exam PDF. Identify and extract each question with its answer options and 
the correct answer (if available).

For each question, return a JSON object with:
- questionNumber: The question number
- questionText: The full question text
- options: Array of {letter, text} objects
- correctAnswer: The correct option letter (if available)
- specialty: Classify into one of [Clínica Médica, Pediatria, Ginecologia e Obstetrícia, 
  Cirurgia Geral, Medicina Preventiva, Psiquiatria, Medicina de Família, Ortopedia, 
  Anestesiologia, Neurologia, Urologia, Oftalmologia, Dermatologia, Otorrinolaringologia]
- subTopic: Specific sub-topic within the specialty
- difficulty: 1-5 (1=easiest, 5=hardest)

Text to analyze:
{extracted_text}
```

**Explanation Generation Prompt:**
```
You are a medical education specialist explaining exam answers. For the following medical 
residency exam question, provide:

1. A detailed explanation of why the correct answer is right (with clinical reasoning)
2. For EACH incorrect option, explain specifically why it is wrong
3. Key high-yield teaching points related to this question
4. Common exam pitfalls related to this topic

Question: {question_text}
Options: {options}
Correct Answer: {correct_answer}
User's Answer: {user_answer}

Format your response in clear sections with headers.
```

---

## 5. UI/UX Design Guidelines

### 5.1 Design Language
- Material 3 with dynamic color theming
- Dark mode support
- Clean, medical-professional aesthetic
- Primary color: Deep teal (#00695C)
- Accent: Warm amber (#FFA000)
- Background: Light gray or dark depending on theme

### 5.2 Key Screens

1. **Splash Screen** → Logo + loading
2. **Onboarding** → 4-step wizard
3. **Home Dashboard** → Quick stats, recent activity, start quiz button
4. **Upload** → PDF selection and processing
5. **Quiz** → Question cards with answer options
6. **Results** → Score summary after quiz completion
7. **Review** → Wrong/flagged questions
8. **Stats** → Analytics dashboard with charts
9. **Notes** → User's notes per question
10. **Settings** → App preferences, profile, data management

### 5.3 Navigation
- Bottom navigation bar: Home, Quiz, Upload, Stats, Profile
- Each with icons and labels
- Back navigation within flows

---

## 6. Metrics & KPIs

### 6.1 User Engagement Metrics
- Daily Active Users (DAU)
- Session length
- Questions answered per session
- Quiz completion rate
- Return rate (D1, D7, D30)
- Streak length distribution

### 6.2 Learning Effectiveness Metrics
- Accuracy improvement over time (by specialty)
- Time-to-answer trends
- Weak area improvement rate
- Question revisit rate
- Note creation frequency

### 6.3 Technical Metrics
- PDF processing time
- API response time
- App crash rate
- Database size growth

---

## 7. Medical Exam Reference Data

### 7.1 ENAM (Exame Nacional de Residência Médica) - Brazil

| Year | Total Questions | Participants | Approval Rate |
|------|----------------|-------------|---------------|
| 2023 | ~120 | ~35,000 | ~30% |
| 2024 | ~120 | ~38,000 | ~28% |

**Distribution:**
- Clínica Médica: 30-35 questions
- Pediatria: 15-18 questions
- Ginecologia/Obstetrícia: 15-18 questions
- Cirurgia Geral: 15-18 questions
- Medicina Preventiva: 12-15 questions
- Others (Psychiatry, Family Med, etc.): 20-25 questions

### 7.2 Difficulty Calibration

Based on historical exam data:
- **Easy** (Difficulty 1): Basic definitions, common presentations, standard protocols
- **Medium-Easy** (Difficulty 2): Clinical reasoning, diagnosis, common treatments
- **Medium** (Difficulty 3): Complex clinical scenarios, differential diagnosis
- **Medium-Hard** (Difficulty 4): Unusual presentations, exceptions to rules, research-based
- **Hard** (Difficulty 5): Edge cases, controversial topics, advanced pathophysiology

### 7.3 Common Exam Topics (High-Yield)

**Top 20 Most Tested Topics (across all exams):**
1. Hypertension management
2. Diabetes mellitus (Type 1 & 2)
3. Acute coronary syndromes
4. Pneumonia (community-acquired)
5. Heart failure
6. Prenatal care & pre-eclampsia
7. Acute abdomen (differential diagnosis)
8. Anemias (classification and treatment)
9. Vaccination schedules
10. HIV/AIDS
11. Stroke (ischemic vs hemorrhagic)
12. Asthma & COPD
13. Acute kidney injury
14. Septic shock
15. Fractures (management)
16. Contraception methods
17. Epidemiology (study types, bias)
18. Hepatitis (A, B, C)
19. Electrolyte disorders
20. Pediatric emergencies (bronchiolitis, croup, dehydration)

---

## 8. Future Features (Roadmap)

### Phase 2
- Cloud sync (backup/restore)
- Shared question banks between users
- Spaced repetition algorithm (SM-2)
- PDF OCR for scanned documents
- Widget for daily questions

### Phase 3
- Multi-language support (English, Spanish, Portuguese)
- Audio explanations
- Live exam countdown timers
- Community features (discussions per question)
- Integration with official exam calendars

---

## 9. Competitive Analysis

| Feature | MoonDice | AnkiMed | QBank Pro | MedFlash |
|---------|----------|---------|-----------|----------|
| PDF Import | ✅ | ❌ | ❌ | ❌ |
| AI Explanations | ✅ | ❌ | ❌ | ✅ |
| Subject Analysis | ✅ | ❌ | ✅ | ❌ |
| Local Storage | ✅ | ✅ | ❌ | ❌ |
| Notes/Comments | ✅ | ✅ | ❌ | ✅ |
| Analytics Dashboard | ✅ | ❌ | ✅ | ✅ |
| Free Tier | ✅ | ✅ | ❌ | ❌ |
| Custom Exams | ✅ | ✅ | ❌ | ❌ |

---

## 10. API Cost Estimation

### Cohere API Pricing (command-r-plus)
- Input: $2.50 / 1M tokens
- Output: $10.00 / 1M tokens

### Per-Session Estimates
- **PDF Parsing** (120-question exam): ~8,000 input + 3,000 output tokens ≈ $0.05
- **Explanation Generation**: ~500 input + 800 output tokens per question ≈ $0.009/question
- **10-question quiz with explanations**: ~$0.09

### Monthly Estimate (active user)
- 2 exam uploads: $0.10
- 200 questions answered with explanations: $1.80
- **Total: ~$1.90/month per active user**
