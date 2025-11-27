package org.example.repository;

import org.example.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TrackRepository {
    public void addTrack(int audioId, int startTime, int endTime) {
        String sql = "INSERT INTO Track (audio_id, start_time, end_time) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, audioId);
            statement.setInt(2, startTime);
            statement.setInt(3, endTime);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert track", e);
        }
    }

    public List<String> getAllTracks() {
        String sql = "SELECT * FROM Track";
        List<String> trackList = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                trackList.add("Track ID: " + resultSet.getInt("id") +
                        ", Audio ID: " + resultSet.getInt("audio_id") +
                        ", Start Time: " + resultSet.getInt("start_time") +
                        ", End Time: " + resultSet.getInt("end_time"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch tracks", e);
        }

        return trackList;
    }
}
