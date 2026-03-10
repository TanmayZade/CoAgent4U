-- V9: Add slot columns to coordinations table for slot selection workflow
ALTER TABLE coordinations ADD COLUMN IF NOT EXISTS available_slots_json JSONB;
ALTER TABLE coordinations ADD COLUMN IF NOT EXISTS selected_slot_json JSONB;
