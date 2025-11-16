-- ============================================================
-- VEHICLE TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS vehicle (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL
);

-- ============================================================
-- CLEANER PROFESSIONAL TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS cleaner_professional (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    vehicle_id BIGINT NOT NULL,
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(id)
        ON DELETE CASCADE
);

-- ============================================================
-- BOOKING TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS booking (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    start_datetime DATETIME NOT NULL,
    end_datetime DATETIME NOT NULL,
    duration_in_hours INT NOT NULL,
    requested_cleaner_count INT NOT NULL
);

-- ============================================================
-- BOOKING-CLEANER MAPPING TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS booking_cleaner (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    cleaner_id BIGINT NOT NULL,
    FOREIGN KEY (booking_id) REFERENCES booking(id)
        ON DELETE CASCADE,
    FOREIGN KEY (cleaner_id) REFERENCES cleaner_professional(id)
        ON DELETE CASCADE
);

-- ============================================================
-- ADD UNIQUE CONSTRAINT SAFELY (booking_id, cleaner_id)
-- ============================================================

SET @exists := (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE table_name = 'booking_cleaner'
      AND constraint_name = 'uq_booking_cleaner'
);

SET @sql := IF(
    @exists = 0,
    'ALTER TABLE booking_cleaner ADD CONSTRAINT uq_booking_cleaner UNIQUE (booking_id, cleaner_id)',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- AVAILABILITY BLOCKS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS availability_blocks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cleaner_id BIGINT NOT NULL,
    start_datetime DATETIME NOT NULL,
    end_datetime DATETIME NOT NULL,
    block_type VARCHAR(20) NOT NULL,  -- BOOKED or BREAK
    FOREIGN KEY (cleaner_id) REFERENCES cleaner_professional(id)
        ON DELETE CASCADE
);

-- ============================================================
-- ADD UNIQUE CONSTRAINT SAFELY FOR BLOCKS
-- ============================================================

SET @exists2 := (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE table_name = 'availability_blocks'
      AND constraint_name = 'uq_cleaner_block'
);

SET @sql2 := IF(
    @exists2 = 0,
    'ALTER TABLE availability_blocks ADD CONSTRAINT uq_cleaner_block UNIQUE (cleaner_id, start_datetime, end_datetime, block_type)',
    'SELECT 1'
);

PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;