-- ============================================================
-- INSERT VEHICLES (IDEMPOTENT)
-- ============================================================

INSERT INTO vehicle (name)
SELECT 'DXB-Vehicle-101'
WHERE NOT EXISTS (SELECT 1 FROM vehicle WHERE name='DXB-Vehicle-101');

INSERT INTO vehicle (name)
SELECT 'DXB-Vehicle-102'
WHERE NOT EXISTS (SELECT 1 FROM vehicle WHERE name='DXB-Vehicle-102');

INSERT INTO vehicle (name)
SELECT 'DXB-Vehicle-103'
WHERE NOT EXISTS (SELECT 1 FROM vehicle WHERE name='DXB-Vehicle-103');

INSERT INTO vehicle (name)
SELECT 'DXB-Vehicle-104'
WHERE NOT EXISTS (SELECT 1 FROM vehicle WHERE name='DXB-Vehicle-104');

INSERT INTO vehicle (name)
SELECT 'DXB-Vehicle-105'
WHERE NOT EXISTS (SELECT 1 FROM vehicle WHERE name='DXB-Vehicle-105');


-- ============================================================
-- INSERT CLEANERS PER VEHICLE (IDEMPOTENT)
-- ============================================================

-- ------------- Vehicle 1 -------------
INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Ayesha Khan', 1
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Ayesha Khan' AND vehicle_id=1);

INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Rashid Mohammed', 1
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Rashid Mohammed' AND vehicle_id=1);

INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Priya Nair', 1
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Priya Nair' AND vehicle_id=1);

INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Sanjay Verma', 1
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Sanjay Verma' AND vehicle_id=1);

INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Anita Desai', 1
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Anita Desai' AND vehicle_id=1);


-- ------------- Vehicle 2 -------------
INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Deepak Singh', 2
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Deepak Singh' AND vehicle_id=2);

INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Farah Al Mansoori', 2
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Farah Al Mansoori' AND vehicle_id=2);

INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Vikram Reddy', 2
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Vikram Reddy' AND vehicle_id=2);

INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Neha Gupta', 2
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Neha Gupta' AND vehicle_id=2);

INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Mohammed Irfan', 2
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Mohammed Irfan' AND vehicle_id=2);


-- ------------- Vehicle 3 -------------
INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Arjun Sharma', 3
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Arjun Sharma' AND vehicle_id=3);

INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Sara Mathew', 3
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Sara Mathew' AND vehicle_id=3);

INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Nikhil Patil', 3
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Nikhil Patil' AND vehicle_id=3);

INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Zainab Ali', 3
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Zainab Ali' AND vehicle_id=3);

INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Harish Kumar', 3
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Harish Kumar' AND vehicle_id=3);


-- ------------- Vehicle 4 -------------
INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Kavita Joshi', 4
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Kavita Joshi' AND vehicle_id=4);

INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Sameer Sheikh', 4
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Sameer Sheikh' AND vehicle_id=4);

INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Reema Choudhary', 4
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Reema Choudhary' AND vehicle_id=4);

INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Rohan Mehta', 4
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Rohan Mehta' AND vehicle_id=4);

INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Suhail Rahman', 4
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Suhail Rahman' AND vehicle_id=4);


-- ------------- Vehicle 5 -------------
INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Pooja Suresh', 5
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Pooja Suresh' AND vehicle_id=5);

INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Adnan Khalid', 5
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Adnan Khalid' AND vehicle_id=5);

INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Shruti Iyer', 5
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Shruti Iyer' AND vehicle_id=5);

INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Aftab Mirza', 5
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Aftab Mirza' AND vehicle_id=5);

INSERT INTO cleaner_professional (name, vehicle_id)
SELECT 'Manish Tiwari', 5
WHERE NOT EXISTS (SELECT 1 FROM cleaner_professional WHERE name='Manish Tiwari' AND vehicle_id=5);