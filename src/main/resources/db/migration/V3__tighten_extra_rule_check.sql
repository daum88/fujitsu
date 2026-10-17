-- Delivery Fee Service schema (Flyway V3)
-- Tighten extra_fee_rule so the condition payload always matches the condition type.

ALTER TABLE extra_fee_rule
    ADD CONSTRAINT chk_extra_rule_condition_type CHECK (
        (condition_type = 'PHENOMENON' AND phenomenon_category IS NOT NULL) OR
        (condition_type IN ('TEMPERATURE', 'WIND') AND phenomenon_category IS NULL)
    );
