-- Delivery Fee Service seed rules (Flyway V2)
-- Base fees: 3 cities x 3 vehicles. Extra fees: temperature, wind, phenomenon.
-- Validity windows are half-open [valid_from, valid_to); NULL valid_to = open-ended.

-- ===== Regional base fees (RBF) =====
INSERT INTO base_fee_rule (city, vehicle_type, fee, valid_from) VALUES
    ('TALLINN', 'CAR',    4.00, TIMESTAMP WITH TIME ZONE '2000-01-01 00:00:00+00'),
    ('TALLINN', 'SCOOTER', 3.50, TIMESTAMP WITH TIME ZONE '2000-01-01 00:00:00+00'),
    ('TALLINN', 'BIKE',    3.00, TIMESTAMP WITH TIME ZONE '2000-01-01 00:00:00+00'),
    ('TARTU',   'CAR',     3.50, TIMESTAMP WITH TIME ZONE '2000-01-01 00:00:00+00'),
    ('TARTU',   'SCOOTER', 3.00, TIMESTAMP WITH TIME ZONE '2000-01-01 00:00:00+00'),
    ('TARTU',   'BIKE',    2.50, TIMESTAMP WITH TIME ZONE '2000-01-01 00:00:00+00'),
    ('PARNU',   'CAR',     3.00, TIMESTAMP WITH TIME ZONE '2000-01-01 00:00:00+00'),
    ('PARNU',   'SCOOTER', 2.50, TIMESTAMP WITH TIME ZONE '2000-01-01 00:00:00+00'),
    ('PARNU',   'BIKE',    2.00, TIMESTAMP WITH TIME ZONE '2000-01-01 00:00:00+00');

-- ===== Air temperature extra fee (ATEF) — scooter & bike =====
-- t < -10 -> +1.00
INSERT INTO extra_fee_rule (condition_type, vehicle_type, max_value, max_inclusive, fee, valid_from) VALUES
    ('TEMPERATURE', 'SCOOTER', -10.0, FALSE, 1.00, TIMESTAMP WITH TIME ZONE '2000-01-01 00:00:00+00'),
    ('TEMPERATURE', 'BIKE',    -10.0, FALSE, 1.00, TIMESTAMP WITH TIME ZONE '2000-01-01 00:00:00+00');
-- -10 <= t <= 0 -> +0.50
INSERT INTO extra_fee_rule (condition_type, vehicle_type, min_value, min_inclusive, max_value, max_inclusive, fee, valid_from) VALUES
    ('TEMPERATURE', 'SCOOTER', -10.0, TRUE, 0.0, TRUE, 0.50, TIMESTAMP WITH TIME ZONE '2000-01-01 00:00:00+00'),
    ('TEMPERATURE', 'BIKE',    -10.0, TRUE, 0.0, TRUE, 0.50, TIMESTAMP WITH TIME ZONE '2000-01-01 00:00:00+00');

-- ===== Wind speed extra fee (WSEF) — bike only =====
-- 10 <= w <= 20 -> +0.50
INSERT INTO extra_fee_rule (condition_type, vehicle_type, min_value, min_inclusive, max_value, max_inclusive, fee, valid_from) VALUES
    ('WIND', 'BIKE', 10.0, TRUE, 20.0, TRUE, 0.50, TIMESTAMP WITH TIME ZONE '2000-01-01 00:00:00+00');
-- w > 20 -> forbidden
INSERT INTO extra_fee_rule (condition_type, vehicle_type, min_value, min_inclusive, forbidden, valid_from) VALUES
    ('WIND', 'BIKE', 20.0, FALSE, TRUE, TIMESTAMP WITH TIME ZONE '2000-01-01 00:00:00+00');

-- ===== Weather phenomenon extra fee (WPEF) — scooter & bike =====
-- Snow / sleet -> +1.00
INSERT INTO extra_fee_rule (condition_type, vehicle_type, phenomenon_category, fee, valid_from) VALUES
    ('PHENOMENON', 'SCOOTER', 'SNOW_SLEET', 1.00, TIMESTAMP WITH TIME ZONE '2000-01-01 00:00:00+00'),
    ('PHENOMENON', 'BIKE',    'SNOW_SLEET', 1.00, TIMESTAMP WITH TIME ZONE '2000-01-01 00:00:00+00');
-- Rain -> +0.50
INSERT INTO extra_fee_rule (condition_type, vehicle_type, phenomenon_category, fee, valid_from) VALUES
    ('PHENOMENON', 'SCOOTER', 'RAIN', 0.50, TIMESTAMP WITH TIME ZONE '2000-01-01 00:00:00+00'),
    ('PHENOMENON', 'BIKE',    'RAIN', 0.50, TIMESTAMP WITH TIME ZONE '2000-01-01 00:00:00+00');
-- Glaze / hail / thunder -> forbidden
INSERT INTO extra_fee_rule (condition_type, vehicle_type, phenomenon_category, forbidden, valid_from) VALUES
    ('PHENOMENON', 'SCOOTER', 'GLAZE_HAIL_THUNDER', TRUE, TIMESTAMP WITH TIME ZONE '2000-01-01 00:00:00+00'),
    ('PHENOMENON', 'BIKE',    'GLAZE_HAIL_THUNDER', TRUE, TIMESTAMP WITH TIME ZONE '2000-01-01 00:00:00+00');
