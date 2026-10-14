package com.smartshield.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

/**
 * Deterministic local knowledge base for civic processes, public grievances,
 * emergency protocols, basic legal rights, and Smart Shield platform assistance.
 * Operates as a reliable fallback when Gemini is unconfigured or unreachable.
 */
@Service
public class LegalKnowledgeBaseService {

    public static class KnowledgeMatch {
        private final String answer;
        private final List<String> suggestedActions;
        private final boolean isEmergency;

        public KnowledgeMatch(String answer, List<String> suggestedActions, boolean isEmergency) {
            this.answer = answer;
            this.suggestedActions = suggestedActions;
            this.isEmergency = isEmergency;
        }

        public String getAnswer() {
            return answer;
        }

        public List<String> getSuggestedActions() {
            return suggestedActions;
        }

        public boolean isEmergency() {
            return isEmergency;
        }
    }

    /**
     * Inspects citizen message and returns deterministic, verified guidance.
     */
    public KnowledgeMatch findRelevantAnswer(String query) {
        if (query == null || query.isBlank()) {
            return new KnowledgeMatch(
                    "Hello! I am your Smart Shield Civic & Legal Assistant. How can I assist you with public grievances, city services, safety routes, or basic legal information today?",
                    List.of("File a Grievance", "Check Safe Routes", "Emergency SOS Helplines"),
                    false
            );
        }

        String q = query.toLowerCase(Locale.ROOT).trim();

        // 1. Immediate Emergency Check
        if (isEmergencyQuery(q)) {
            return new KnowledgeMatch(
                    "🚨 IMMEDIATE SAFETY NOTICE:\n\n" +
                    "If you are facing active violence, assault, stalking, or an immediate life-threatening emergency, please take action immediately:\n\n" +
                    "• CALL 112: All-in-One National Emergency Response Support System (Police, Fire, Ambulance).\n" +
                    "• CALL 1091: Women Safety Helpline (24/7 dedicated support).\n" +
                    "• CALL 1090: Anti-Stalking and Harassment Helpline.\n" +
                    "• USE SMART SHIELD SOS: Open the Women Safety portal and press the Emergency SOS button to broadcast your GPS location to your registered emergency contacts and local monitoring units.",
                    List.of("Call 112 Emergency", "Open Women Safety SOS", "Helpline Directory"),
                    true
            );
        }

        // 2. Complaint Drafting Assistance
        if (q.contains("draft") || q.contains("write a complaint") || q.contains("how to write") || q.contains("template")) {
            return draftComplaintTemplate(q);
        }

        // 3. Grievance Process & Tracking
        if (q.contains("tracking id") || q.contains("track") || q.contains("status")) {
            return new KnowledgeMatch(
                    "Smart Shield Grievance Tracking Information:\n\n" +
                    "• Tracking ID Format: Every complaint receives a unique tracking ID such as SS-2026-0001 upon creation.\n" +
                    "• Status Workflow: Complaints progress from PENDING → UNDER_REVIEW → IN_PROGRESS → RESOLVED (or REJECTED with officer remarks).\n" +
                    "• Audit History: Every status transition records the timestamp, administrative actor, and official remarks for civic transparency.\n" +
                    "• Check Status: Navigate to 'My Grievances' in the sidebar to review live updates and resolution notes.",
                    List.of("View My Grievances", "Raise New Complaint"),
                    false
            );
        }

        if (q.contains("file a grievance") || q.contains("submit complaint") || q.contains("how to complain") || q.contains("raise")) {
            return new KnowledgeMatch(
                    "How to File a Grievance on Smart Shield:\n\n" +
                    "1. Go to 'Raise Complaint' from the sidebar navigation.\n" +
                    "2. Select the relevant Category: Public Safety, Roads, Sanitation, Water Supply, Electricity, or Corruption.\n" +
                    "3. Enter a clear Title and describe the issue with specific location landmarks.\n" +
                    "4. Attach Supporting Evidence: You can upload photos or documents (PNG, JPG, PDF) to substantiate your grievance.\n" +
                    "5. Submit: Your grievance is assigned a permanent tracking ID and routed to civic authorities for action.",
                    List.of("Raise Complaint Now", "Evidence Guidelines", "Category Guide"),
                    false
            );
        }

        // 4. Safe Routes & Navigation
        if (q.contains("safe route") || q.contains("safety score") || q.contains("score") || q.contains("walking")) {
            return new KnowledgeMatch(
                    "Smart Shield Safe Routes & Safety Scoring:\n\n" +
                    "• How It Works: Safe Routes evaluates transit corridors between your origin and destination using verified civic safety signals.\n" +
                    "• Safety Factors Evaluated:\n" +
                    "   - Civic Grievance Density: Presence of reported dark spots or public hazards.\n" +
                    "   - Emergency SOS History: Anonymized historical SOS incident density in the sector.\n" +
                    "   - Community Reviews: Star ratings and safety tags (Well Lit, Police Patrol, Isolated) from verified citizens.\n" +
                    "   - Emergency Infrastructure: Proximity to police stations and medical centers.\n" +
                    "   - Night Walking Factor: Automated caution advisories for late-night pedestrian transit.\n" +
                    "• Safety Levels: 80–100 (Very Safe), 60–79 (Safe), 40–59 (Moderate), 20–39 (Risky), 0–19 (High Risk).",
                    List.of("Open Safe Routes", "Calculate a Route", "Rate a Route"),
                    false
            );
        }

        // 5. Women Safety & SOS Center
        if (q.contains("sos") || q.contains("women safety") || q.contains("emergency contact") || q.contains("telegram")) {
            return new KnowledgeMatch(
                    "Women Safety Center & Emergency SOS Guide:\n\n" +
                    "• Emergency SOS: Pressing the SOS button instantly captures your browser GPS coordinates and logs an emergency alert.\n" +
                    "• Emergency Contacts: Add trusted family members or friends with their phone number, email, and relationship.\n" +
                    "• Telegram Bot Alerts: Link your contacts to the Smart Shield Telegram Bot using single-use connection codes (e.g. SH-XXXXXX). When you trigger SOS, connected contacts receive instant Telegram alerts with clickable Google Maps location links.\n" +
                    "• Admin Monitoring: Municipal dispatch units monitor active SOS alerts in real time.",
                    List.of("Open Women Safety Center", "Add Emergency Contact", "Connect Telegram Bot"),
                    false
            );
        }

        // 6. Civic Categories: Roads, Streetlights, Sanitation, Water, Electricity
        if (q.contains("road") || q.contains("pothole") || q.contains("traffic")) {
            return new KnowledgeMatch(
                    "Filing Road & Pothole Grievances:\n\n" +
                    "• Category: Select 'ROADS' in the complaint form.\n" +
                    "• Key Details to Include: Exact street name, nearest cross or landmark, depth/size of the pothole, and whether it causes waterlogging or traffic accidents.\n" +
                    "• Evidence: Photos taken from multiple angles showing the road damage significantly expedite engineering inspection.\n" +
                    "• Department: Forwarded to Municipal Road Maintenance & Highway Engineering Division.",
                    List.of("Draft Road Complaint", "Raise Road Grievance"),
                    false
            );
        }

        if (q.contains("light") || q.contains("dark") || q.contains("streetlight") || q.contains("electricity") || q.contains("power")) {
            return new KnowledgeMatch(
                    "Streetlight & Electricity Grievance Guidance:\n\n" +
                    "• Dark Spots / Faulty Streetlights: File under 'PUBLIC_SAFETY' or 'ELECTRICITY'.\n" +
                    "• Why It Matters: Adequate street lighting is a primary crime deterrent and pedestrian safety factor.\n" +
                    "• Details to Mention: Pole number (if visible), exact stretch between landmarks, and how long the light has been non-functional.\n" +
                    "• Safe Routes Integration: Unresolved lighting complaints dynamically update nearby Safe Route safety scores.",
                    List.of("Report Dark Spot", "Raise Electricity Complaint"),
                    false
            );
        }

        if (q.contains("garbage") || q.contains("waste") || q.contains("drain") || q.contains("sewage") || q.contains("sanitation")) {
            return new KnowledgeMatch(
                    "Sanitation & Public Hygiene Complaints:\n\n" +
                    "• Category: Select 'SANITATION' in the complaint form.\n" +
                    "• Common Issues: Overflowing public dustbins, uncleared commercial waste, blocked storm-water drains, sewage leakage.\n" +
                    "• Municipal Standard: Most civic corporations mandate clearing public garbage dumps within 24 to 48 hours of ticket logging.\n" +
                    "• Recommendation: Attach a photo and exact GPS/landmark location for garbage pickup dispatch.",
                    List.of("Raise Sanitation Complaint"),
                    false
            );
        }

        if (q.contains("corruption") || q.contains("bribe")) {
            return new KnowledgeMatch(
                    "Anti-Corruption Guidance & Citizen Rights:\n\n" +
                    "• Smart Shield Category: File under 'CORRUPTION'.\n" +
                    "• Legal Protection: The Prevention of Corruption Act, 1988 penalizes demanding or taking bribes by public servants.\n" +
                    "• Reporting Checklist: Record date, time, government office/department, official designation, demanded sum, and copies of any official receipts or correspondence.\n" +
                    "• Confidentiality: Internal administrative review safeguards citizen identities during investigation.",
                    List.of("File Anti-Corruption Report", "RTI Information"),
                    false
            );
        }

        // 7. General Legal Rights: FIR, RTI, Consumer
        if (q.contains("fir") || q.contains("police complaint") || q.contains("police station")) {
            return new KnowledgeMatch(
                    "Basic Legal Information — Police Complaints & FIR (India):\n\n" +
                    "• Cognizable Offense: Under Section 154 of CrPC (and BNSS), police are legally obligated to register an FIR for cognizable offenses (e.g. theft, physical assault, robbery).\n" +
                    "• Zero FIR: A citizen can lodge a Zero FIR at ANY police station regardless of jurisdiction if an incident occurred elsewhere; it is later transferred to the jurisdiction police.\n" +
                    "• Right to a Free Copy: As the complainant, you are legally entitled to receive a free physical copy of the registered FIR immediately.\n" +
                    "• Disclaimer: This is general legal information. For individual criminal proceedings, consult a practicing advocate.",
                    List.of("Emergency Helplines", "Raise Public Safety Complaint"),
                    false
            );
        }

        if (q.contains("rti") || q.contains("right to information")) {
            return new KnowledgeMatch(
                    "Right to Information (RTI Act, 2005) Quick Guide:\n\n" +
                    "• Purpose: Enables any Indian citizen to request information from government departments and public authorities.\n" +
                    "• Timeline: The Public Information Officer (PIO) must respond within 30 days of application (within 48 hours if related to life or liberty).\n" +
                    "• Grievance Escalation: If a civic complaint remains ignored for an extended period, an RTI can be filed inquiring about official action taken on your complaint tracking ID.\n" +
                    "• Official Portal: Online RTI applications can be filed at rtionline.gov.in.",
                    List.of("View My Grievances"),
                    false
            );
        }

        // 8. Default Civic Guidance
        return new KnowledgeMatch(
                "Smart Shield Civic Assistant Information:\n\n" +
                "I can guide you on:\n" +
                "• Raising civic grievances (Roads, Sanitation, Electricity, Public Safety, Corruption).\n" +
                "• Tracking complaint progress with your tracking ID.\n" +
                "• Drafting professional complaint descriptions for municipal submission.\n" +
                "• Using the Women Safety Center & Emergency SOS dispatch.\n" +
                "• Understanding Safe Routes transit safety scores.\n\n" +
                "Please type a specific query or question, and I will be happy to provide guidance.",
                List.of("Draft a Complaint", "Safe Routes Guide", "Emergency Helpline Info"),
                false
        );
    }

    private boolean isEmergencyQuery(String q) {
        String[] emergencyKeywords = {
            "emergency", "attack", "stalker", "stalking", "danger", "violent",
            "violence", "threat", "assault", "harass", "help me now", "suicide",
            "fire emergency", "bleeding", "kidnap", "weapon"
        };
        for (String kw : emergencyKeywords) {
            if (q.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private KnowledgeMatch draftComplaintTemplate(String q) {
        String issue = "Public Infrastructure / Civic Maintenance Issue";
        String category = "PUBLIC_SAFETY";

        if (q.contains("road") || q.contains("pothole")) {
            issue = "Hazardous Road Surface & Pothole Repair";
            category = "ROADS";
        } else if (q.contains("light") || q.contains("dark")) {
            issue = "Non-Functional Streetlights & Dark Spot Hazard";
            category = "PUBLIC_SAFETY";
        } else if (q.contains("garbage") || q.contains("sanitation")) {
            issue = "Accumulated Waste & Public Sanitation Hazard";
            category = "SANITATION";
        } else if (q.contains("water")) {
            issue = "Contaminated Water Supply / Pipeline Leakage";
            category = "WATER_SUPPLY";
        } else if (q.contains("electricity") || q.contains("power")) {
            issue = "Exposed Wiring & Frequent Power Outage";
            category = "ELECTRICITY";
        }

        String draft = String.format(
                "Here is a structured complaint draft ready for you to review and submit:\n\n" +
                "--------------------------------------------------\n" +
                "TITLE: Urgent Attention Required: %s\n" +
                "CATEGORY: %s\n" +
                "PRIORITY: HIGH\n" +
                "LOCATION: [Insert Exact Street Name / Landmark, e.g. 5th Main Road, near Metro Pillar 124]\n\n" +
                "DESCRIPTION:\n" +
                "I am writing to bring to your urgent attention a serious civic issue regarding %s at the aforementioned location. " +
                "This condition has persisted for [Insert duration, e.g., over 2 weeks] and poses an active safety and transit hazard to daily commuters, elderly residents, and school children.\n\n" +
                "IMPACT:\n" +
                "• Severe risk of vehicular accidents and pedestrian falls.\n" +
                "• Reduced evening transit safety for women and commuters.\n\n" +
                "REQUESTED ACTION:\n" +
                "Kindly dispatch an inspection team and initiate corrective maintenance at the earliest. " +
                "Photographic evidence has been attached to assist municipal teams.\n" +
                "--------------------------------------------------\n\n" +
                "Note: You can copy this text, adjust the location specifics, and paste it into the 'Raise Complaint' form.",
                issue, category, issue.toLowerCase(Locale.ROOT)
        );

        return new KnowledgeMatch(
                draft,
                List.of("Open Raise Complaint Form", "Evidence Upload Guide"),
                false
        );
    }
}
