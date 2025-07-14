package com.blooddonation.blood_donation_support_system.enums;

public enum MedicalCondition {
    TRAUMA_EMERGENCY_SURGERY("Trauma/Emergency Surgery", "Severe injury requiring immediate surgical intervention", Urgency.HIGH),
    SEVERE_ANEMIA("Severe Anemia", "Critically low hemoglobin levels", Urgency.HIGH),
    ACTIVE_BLEEDING("Active Bleeding", "Ongoing blood loss from internal or external sources", Urgency.HIGH),
    CARDIAC_SURGERY("Cardiac Surgery", "Heart surgery requiring blood products", Urgency.MEDIUM),
    CANCER_TREATMENT("Cancer Treatment", "Chemotherapy-induced blood disorders", Urgency.MEDIUM),
    ORGAN_TRANSPLANT("Organ Transplant", "Major organ transplantation procedure", Urgency.MEDIUM),
    PREGNANCY_COMPLICATIONS("Pregnancy Complications", "Maternal or fetal complications requiring blood", Urgency.MEDIUM),
    PLANNED_SURGERY("Planned Surgery", "Elective surgical procedure", Urgency.LOW),
    BLOOD_DISORDER("Blood Disorder", "Chronic blood-related conditions", Urgency.LOW);

    private final String condition;
    private final String description;
    private final Urgency urgencyLevel;

    MedicalCondition(String condition, String description, Urgency urgencyLevel) {
        this.condition = condition;
        this.description = description;
        this.urgencyLevel = urgencyLevel;
    }

    public String getCondition() {
        return condition;
    }

    public String getDescription() {
        return description;
    }

    public Urgency getUrgencyLevel() {
        return urgencyLevel;
    }
}
