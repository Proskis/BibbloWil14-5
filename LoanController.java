
package com.mycompany.bibbliotekcaseWIL;

import java.io.IOException;
import java.sql.Connection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.layout.AnchorPane;
import java.sql.CallableStatement;
import java.sql.SQLException;

/**
 *
 * @author William Visscher
 */
public class LoanController {
    private Parent root;
    private Scene scene;
    private Stage stage;

    private Connection connection;
    
    @FXML
    public TextField itemIdField;

    @FXML
    private TextField userIdField;
    
    @FXML
    private void initialize(){
        setUserIdField();
    }
    
    public void setUserIdField(){
    userIdField.setText(String.valueOf(Session.userID));
}
    
     @FXML
    public void LoanItem(ActionEvent event) throws IOException { 
        String itemId = itemIdField.getText().trim();
        String userId = userIdField.getText().trim();
     
        if (itemId.isEmpty() || userId.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error:", "ItemID är nödvändigt för ett lån");
           return;
        }
        
        if(!itemExists(itemId)){
            showAlert(Alert.AlertType.ERROR, "Error", "Artikeln finns inte i databasen");
                       return;
        }
        
        if (!itemInStock(itemId)){
            showAlert (Alert.AlertType.ERROR, "Error", "Artikeln du försöker låna är slut i lager");
                       return;
        }
        
        if (userHasReachedLoanLimit(userId)){
            showAlert (Alert.AlertType.ERROR, "Error", "Du har lånat ditt maxantal lån. Returnera ett lån för att göra ett nytt lån");
                       return;
        }
        
        String sql = "CALL bibliotek.new_loan(?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
         CallableStatement stmt = conn.prepareCall(sql)) {

        stmt.setString(1, itemId);
        stmt.setString(2, userId);
        stmt.execute();

        showAlert(Alert.AlertType.INFORMATION, "Success", "Tack för ditt lån!");
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("KvittoScene.fxml"));
        
        root = loader.load();
        stage = (Stage) scenePane.getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

    } catch (SQLException e) {
        
        e.printStackTrace();
        showAlert(Alert.AlertType.ERROR, "Database Error", "Något gick fel");
    }
    }
    private boolean itemExists(String itemId){
          String sql = "SELECT 1 FROM item WHERE ItemID = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, itemId);
        java.sql.ResultSet rs = stmt.executeQuery();
        return rs.next();
        
    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
}
    
     private boolean itemInStock(String itemId){
          String sql = "SELECT Stock FROM item WHERE ItemID = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, itemId);
        java.sql.ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getInt("Stock") > 0;
        } else {
            return false;
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
      return false;
     }
    private boolean userHasReachedLoanLimit(String userId) {
    String sql = "SELECT ut.MaxLoan, (SELECT COUNT(*) FROM currentloan cl WHERE cl.UserId = u.UserId)"
            + " AS current_loans FROM user u JOIN usertype ut ON u.UserTypeID = ut.UserTypeID WHERE u.UserId = ?;";
    
    
    try (Connection conn = DatabaseConnection.getConnection();
         java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        stmt.setString(1, userId);
        java.sql.ResultSet rs = stmt.executeQuery();
        
        if (rs.next()) {
            int maxLoan = rs.getInt("MaxLoan");
            int currentLoans = rs.getInt("current_loans");
            return currentLoans >= maxLoan;
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return false;
}
    @FXML
    private AnchorPane scenePane;
    
     public void OpenLoanReturnLoanPage(ActionEvent event) throws IOException {
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("LoanReturnLoanScene.fxml"));
        
        root = loader.load();
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        
        stage.setScene(scene);
        stage.show();
    }
    public void logout(ActionEvent event) throws IOException {
          Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("You are logging out");
        alert.setContentText("Are you sure? ");
        
        if(alert.showAndWait().get() == ButtonType.OK) {
            
            Session.isManager = false;
            Session.isStaff = false;
            Session.isUser = false;
            Session.loggedInName = null;
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("LoginScene.fxml"));
        
            root = loader.load();
            stage = (Stage) scenePane.getScene().getWindow();
            scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
            
            System.out.println("Logged out!");
}
    }
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
}
}
