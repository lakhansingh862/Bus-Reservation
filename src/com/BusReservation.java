package com;
import java.sql.*;
import java.util.Scanner;

public class BusReservation {
    static final String URL = "jdbc:mysql://localhost:3306/bus_reservation";
    static final String DB_USER = "root";
    static final String DB_PASSWORD = "14367898Lm&";

    static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, DB_USER, DB_PASSWORD);
    }

    static void addBus(Scanner sc) {
        String sql = "INSERT INTO buses(bus_number,source_city,destination_city,total_seats) VALUES(?,?,?,?)";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            System.out.print("Bus number: "); ps.setString(1, sc.nextLine());
            System.out.print("Source: "); ps.setString(2, sc.nextLine());
            System.out.print("Destination: "); ps.setString(3, sc.nextLine());
            System.out.print("Total seats: "); ps.setInt(4, Integer.parseInt(sc.nextLine()));
            ps.executeUpdate();
            System.out.println("Bus added.");
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); }
    }

    static void searchBuses(Scanner sc) {
        String sql = "SELECT * FROM buses WHERE source_city=? AND destination_city=?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            System.out.print("Source: "); ps.setString(1, sc.nextLine());
            System.out.print("Destination: "); ps.setString(2, sc.nextLine());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("Bus ID: %d | %s | %s -> %s | Seats: %d%n",
                        rs.getInt("bus_id"), rs.getString("bus_number"),
                        rs.getString("source_city"), rs.getString("destination_city"),
                        rs.getInt("total_seats"));
                }
            }
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); }
    }

    static void bookSeat(Scanner sc) {

        Connection con = null;

        try {
            con = getConnection();
            con.setAutoCommit(false);

            System.out.print("Bus ID: ");
            int busId = Integer.parseInt(sc.nextLine());

            // Get total seats
            int totalSeats;

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT total_seats FROM buses WHERE bus_id=? FOR UPDATE")) {

                ps.setInt(1, busId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next())
                        throw new SQLException("Bus not found.");

                    totalSeats = rs.getInt("total_seats");
                }
            }

            System.out.print("How many passengers: ");
            int passengerCount = Integer.parseInt(sc.nextLine());

            if (passengerCount <= 0 || passengerCount > totalSeats) {
                throw new SQLException("Invalid number of passengers.");
            }

            java.util.Set<Integer> selectedSeats = new java.util.HashSet<>();

            // Enter details for each passenger
            for (int i = 1; i <= passengerCount; i++) {

                System.out.println("\n--- Passenger " + i + " ---");

                System.out.print("Passenger name: ");
                String name = sc.nextLine();

                System.out.print("Passenger phone: ");
                String phone = sc.nextLine();

                System.out.print("Seat number: ");
                int seat = Integer.parseInt(sc.nextLine());

                // Check seat number
                if (seat < 1 || seat > totalSeats)
                    throw new SQLException("Invalid seat number.");

                // Check duplicate seat
                if (selectedSeats.contains(seat))
                    throw new SQLException("Seat " + seat + " selected twice.");

                selectedSeats.add(seat);

                // Check already booked seat
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT booking_id FROM bookings WHERE bus_id=? AND seat_number=?")) {

                    ps.setInt(1, busId);
                    ps.setInt(2, seat);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (rs.next())
                            throw new SQLException(
                                    "Seat " + seat + " is already booked.");
                    }
                }

                // Insert passenger
                int passengerId;

                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO passengers(name,phone) VALUES(?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {

                    ps.setString(1, name);
                    ps.setString(2, phone);

                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {

                        rs.next();
                        passengerId = rs.getInt(1);
                    }
                }

                // Insert booking
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO bookings(bus_id,passenger_id,seat_number) VALUES(?,?,?)")) {

                    ps.setInt(1, busId);
                    ps.setInt(2, passengerId);
                    ps.setInt(3, seat);

                    ps.executeUpdate();
                }
            }

            con.commit();

            System.out.println("\nBooking successful!");
            System.out.println("Passengers booked: " + passengerCount);
            System.out.println("Seats: " + selectedSeats);

        } catch (Exception e) {

            try {
                if (con != null)
                    con.rollback();
            } catch (SQLException i) {
                i.printStackTrace();
            }

            System.out.println("Booking failed. All changes rolled back.");
            System.out.println("Reason: " + e.getMessage());

        } finally {

            try {
                if (con != null)
                    con.close();
            } catch (SQLException ignored) {
            }
        }
    }

    static void cancelBooking(Scanner sc) {
        String sql = "DELETE FROM bookings WHERE booking_id=?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            System.out.print("Booking ID: ");
            ps.setInt(1, Integer.parseInt(sc.nextLine()));
            if (ps.executeUpdate() == 1)
                System.out.println("Booking cancelled.");
            else
                System.out.println("Booking not found.");
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); }
    }

    static void showBookings(Scanner sc) {
        String sql = """
            SELECT b.booking_id, bu.bus_number, p.name, p.phone,
                   b.seat_number, b.booking_date
            FROM bookings b
            JOIN buses bu ON b.bus_id=bu.bus_id
            JOIN passengers p ON b.passenger_id=p.passenger_id
            WHERE b.bus_id=?
            ORDER BY b.seat_number
            """;
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            System.out.print("Bus ID: ");
            ps.setInt(1, Integer.parseInt(sc.nextLine()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("Booking %d | Bus %s | %s | Seat %d | %s%n",
                        rs.getInt("booking_id"), rs.getString("bus_number"),
                        rs.getString("name"), rs.getInt("seat_number"),
                        rs.getTimestamp("booking_date"));
                }
            }
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n--- BUS RESERVATION ---");
            System.out.println("1. Add Bus");
            System.out.println("2. Search Buses");
            System.out.println("3. Book Seat");
            System.out.println("4. Cancel Booking");
            System.out.println("5. View Bus Bookings");
            System.out.println("0. Exit");
            System.out.print("Choice: ");

            switch (sc.nextLine()) {
                case "1" -> addBus(sc);
                case "2" -> searchBuses(sc);
                case "3" -> bookSeat(sc);
                case "4" -> cancelBooking(sc);
                case "5" -> showBookings(sc);
                case "0" -> { sc.close(); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
}


		// TODO Auto-generated method stub
