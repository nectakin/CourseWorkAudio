package org.example.repository;

import org.example.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProjectRepository {
    public void addProject(String name) {
        String sql = "INSERT INTO Project (name) VALUES (?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert project", e);
        }
    }

    public List<String> getAllProjects() {
        String sql = "SELECT * FROM Project";
        List<String> projectList = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                projectList.add("Project ID: " + resultSet.getInt("id") +
                        ", Name: " + resultSet.getString("name") +
                        ", Created At: " + resultSet.getString("created_at"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch projects", e);
        }

        return projectList;
    }

    public void addAudioToProject(int projectId, int audioId) {
        String sql = "INSERT INTO Project_Audio (project_id, audio_id) VALUES (?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projectId);
            statement.setInt(2, audioId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add audio to project", e);
        }
    }
}
