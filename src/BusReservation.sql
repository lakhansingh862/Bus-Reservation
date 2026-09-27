CREATE DATABASE IF NOT EXISTS bus_reservation;
USE bus_reservation;

CREATE TABLE IF NOT EXISTS buses (
    bus_id INT PRIMARY KEY AUTO_INCREMENT,
    bus_number VARCHAR(30) UNIQUE NOT NULL,
    source_city VARCHAR(100) NOT NULL,
    destination_city VARCHAR(100) NOT NULL,
    total_seats INT NOT NULL
);

CREATE TABLE IF NOT EXISTS passengers (
    passenger_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS bookings (
    booking_id INT PRIMARY KEY AUTO_INCREMENT,
    bus_id INT NOT NULL,
    passenger_id INT NOT NULL,
    seat_number INT NOT NULL,
    booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bus_id) REFERENCES buses(bus_id),
    FOREIGN KEY (passenger_id) REFERENCES passengers(passenger_id),
    UNIQUE (bus_id, seat_number)
);
