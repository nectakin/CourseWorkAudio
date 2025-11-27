package org.example.repository;

import org.example.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AudioRepository {
    public void addAudio(String name, String format, String path) {
        String sql = "INSERT INTO Audio (name, format, path) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, format);
            statement.setString(3, path);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert audio", e);
        }
    }

    public List<String> getAllAudio() {
        String sql = "SELECT * FROM Audio";
        List<String> audioList = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                audioList.add(resultSet.getString("name"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch audio", e);
        }

        return audioList;
    }

    public List<String> getAudioByProject(int projectId) {
        String sql = "SELECT Audio.name FROM Audio " +
                "JOIN Project_Audio ON Audio.id = Project_Audio.audio_id " +
                "WHERE Project_Audio.project_id = ?";
        List<String> audioList = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    audioList.add(resultSet.getString("name"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch audio for project", e);
        }

        return audioList;
    }
}
/*AudioRepository*/