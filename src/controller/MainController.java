package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import library.Books;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class MainController implements Initializable {


    @FXML
    private TextField idField;
    @FXML
    private TextField titleField;
    @FXML
    private TextField authorField;
    @FXML
    private TextField yearField;
    @FXML
    private TextField pagesField;


    @FXML
    private Button insertButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;

    // Новые элементы
    @FXML
    private Button exportButton;
    @FXML
    private ComboBox<String> exportFormatComboBox;

    @FXML
    private TableView<Books> TableView;
    @FXML
    private TableColumn<Books, Integer> idColumn;
    @FXML
    private TableColumn<Books, String> titleColumn;
    @FXML
    private TableColumn<Books, String> authorColumn;
    @FXML
    private TableColumn<Books, Integer> yearColumn;
    @FXML
    private TableColumn<Books, Integer> pagesColumn;


    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterComboBox;
    @FXML
    private ComboBox<String> yearFilterComboBox;
    @FXML
    private Pagination pagination;

    private static final int ROWS_PER_PAGE = 10;

    private ObservableList<Books> masterData = FXCollections.observableArrayList();
    private ObservableList<Books> filteredData = FXCollections.observableArrayList();

    @FXML
    private void insertButton() {
        if (validateInput()) {
            String query = "INSERT INTO books (Title, Author, Year, Pages) VALUES(?, ?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement pst = conn.prepareStatement(query)) {
                pst.setString(1, titleField.getText());
                pst.setString(2, authorField.getText());
                pst.setInt(3, Integer.parseInt(yearField.getText()));
                pst.setInt(4, Integer.parseInt(pagesField.getText()));
                pst.executeUpdate();
                showAlert(Alert.AlertType.INFORMATION, "Успех", "Книга добавлена успешно!");
                clearFields();
                refreshData();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось добавить книгу.");
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void updateButton() {
        if (validateInput() && !idField.getText().isEmpty()) {
            String query = "UPDATE books SET Title=?, Author=?, Year=?, Pages=? WHERE ID=?";
            try (Connection conn = getConnection();
                 PreparedStatement pst = conn.prepareStatement(query)) {
                pst.setString(1, titleField.getText());
                pst.setString(2, authorField.getText());
                pst.setInt(3, Integer.parseInt(yearField.getText()));
                pst.setInt(4, Integer.parseInt(pagesField.getText()));
                pst.setInt(5, Integer.parseInt(idField.getText()));
                pst.executeUpdate();
                showAlert(Alert.AlertType.INFORMATION, "Успех", "Книга обновлена успешно!");
                clearFields();
                refreshData();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось обновить книгу.");
                e.printStackTrace();
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "Предупреждение", "Пожалуйста, выберите книгу для обновления.");
        }
    }

    @FXML
    private void deleteButton() {
        if (!idField.getText().isEmpty()) {
            String query = "DELETE FROM books WHERE ID=?";
            try (Connection conn = getConnection();
                 PreparedStatement pst = conn.prepareStatement(query)) {
                pst.setInt(1, Integer.parseInt(idField.getText()));
                pst.executeUpdate();
                showAlert(Alert.AlertType.INFORMATION, "Успех", "Книга удалена успешно!");
                clearFields();
                refreshData();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось удалить книгу.");
                e.printStackTrace();
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "Предупреждение", "Пожалуйста, выберите книгу для удаления.");
        }
    }

    private void initializeDatabase() {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS books ("
                + "Id INT AUTO_INCREMENT PRIMARY KEY, "
                + "Title VARCHAR(255), "
                + "Author VARCHAR(255), "
                + "Year INT, "
                + "Pages INT)";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createTableQuery);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось инициализировать базу данных.");
            e.printStackTrace();
        }
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeDatabase();
        setupTableColumns();
        setupFilterComboBox();
        setupYearFilterComboBox();  // Инициализация фильтра по году
        setupSearchField();
        refreshData();

        // Настройка элементов экспорта
        setupExportFormatComboBox();

        TableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                Books selectedBook = newSelection;
                idField.setText(String.valueOf(selectedBook.getId()));
                titleField.setText(selectedBook.getTitle());
                authorField.setText(selectedBook.getAuthor());
                yearField.setText(String.valueOf(selectedBook.getYear()));
                pagesField.setText(String.valueOf(selectedBook.getPages()));
            }
        });
    }

    private void setupFilterComboBox() {
        filterComboBox.setPromptText("Фильтр по автору");
        filterComboBox.setItems(getUniqueAuthors());
        filterComboBox.getItems().add(0, "Все");
        filterComboBox.setValue("Все");

        filterComboBox.setOnAction(e -> applyFilters());
    }

    private void setupYearFilterComboBox() {
        yearFilterComboBox.setPromptText("Фильтр по году");
        yearFilterComboBox.setItems(getUniqueYears());
        yearFilterComboBox.getItems().add(0, "Все");
        yearFilterComboBox.setValue("Все");

        yearFilterComboBox.setOnAction(e -> applyFilters());
    }

    private ObservableList<String> getUniqueAuthors() {
        ObservableList<String> authors = FXCollections.observableArrayList();
        String query = "SELECT DISTINCT Author FROM books";
        try (Connection connection = getConnection();
             Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                String author = rs.getString("Author");
                if (author != null && !author.trim().isEmpty()) {
                    authors.add(author);
                }
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось получить список авторов.");
            e.printStackTrace();
        }
        return authors;
    }

    private ObservableList<String> getUniqueYears() {
        ObservableList<String> years = FXCollections.observableArrayList();
        String query = "SELECT DISTINCT Year FROM books ORDER BY Year DESC";
        try (Connection connection = getConnection();
             Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                years.add(String.valueOf(rs.getInt("Year")));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось получить список годов.");
            e.printStackTrace();
        }
        return years;
    }

    public Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/library?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            String user = "root";
            String password = "admin";
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Соединение с базой данных успешно!");
        } catch (ClassNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Драйвер MySQL не найден!");
            e.printStackTrace();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Ошибка подключения к базе данных!");
            e.printStackTrace();
        }
        return conn;
    }

    public ObservableList<Books> getBooksList() {
        ObservableList<Books> booksList = FXCollections.observableArrayList();
        String query = "SELECT * FROM books";
        try (Connection connection = getConnection();
             Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                Books books = new Books(
                        rs.getInt("Id"),
                        rs.getString("Title"),
                        rs.getString("Author"),
                        rs.getInt("Year"),
                        rs.getInt("Pages")
                );
                booksList.add(books);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось получить список книг.");
            e.printStackTrace();
        }
        return booksList;
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("year"));
        pagesColumn.setCellValueFactory(new PropertyValueFactory<>("pages"));

        idColumn.setSortable(true);
        titleColumn.setSortable(true);
        authorColumn.setSortable(true);
        yearColumn.setSortable(true);
        pagesColumn.setSortable(true);
    }

    private void setupSearchField() {
        searchField.setPromptText("Поиск по названию или автору");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }

    private void applyFilters() {
        String selectedAuthor = filterComboBox.getValue();
        String selectedYear = yearFilterComboBox.getValue();
        String keyword = searchField.getText().toLowerCase();

        filteredData.setAll(masterData);

        if (selectedAuthor != null && !selectedAuthor.equals("Все")) {
            filteredData.removeIf(book -> !book.getAuthor().equalsIgnoreCase(selectedAuthor));
        }

        if (selectedYear != null && !selectedYear.equals("Все")) {
            try {
                int year = Integer.parseInt(selectedYear);
                filteredData.removeIf(book -> book.getYear() != year);
            } catch (NumberFormatException e) {
                // Игнорировать некорректные значения
            }
        }

        if (keyword != null && !keyword.isEmpty()) {
            filteredData.removeIf(book ->
                    !book.getTitle().toLowerCase().contains(keyword) &&
                            !book.getAuthor().toLowerCase().contains(keyword)
            );
        }

        updatePagination();
    }

    private void refreshData() {
        masterData.setAll(getBooksList());
        filteredData.setAll(masterData);
        filterComboBox.setItems(getUniqueAuthors());
        filterComboBox.getItems().add(0, "Все");
        filterComboBox.setValue("Все");
        yearFilterComboBox.setItems(getUniqueYears());
        yearFilterComboBox.getItems().add(0, "Все");
        yearFilterComboBox.setValue("Все");
        updatePagination();
    }

    private void updatePagination() {
        int pageCount = (int) Math.ceil((double) filteredData.size() / ROWS_PER_PAGE);
        pagination.setPageCount(pageCount > 0 ? pageCount : 1);
        pagination.setCurrentPageIndex(0);
        pagination.setPageFactory(this::createPage);
    }

    private javafx.scene.Node createPage(int pageIndex) {
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, filteredData.size());
        TableView.setItems(FXCollections.observableArrayList(filteredData.subList(fromIndex, toIndex)));
        return new BorderPane(TableView);
    }

    public void showBooks() {
        TableView.setItems(masterData);
    }

    private void clearFields() {
        idField.clear();
        titleField.clear();
        authorField.clear();
        yearField.clear();
        pagesField.clear();
    }

    private boolean validateInput() {
        String errorMessage = "";

        if (titleField.getText() == null || titleField.getText().isEmpty()) {
            errorMessage += "Название книги не может быть пустым!\n";
        }
        if (authorField.getText() == null || authorField.getText().isEmpty()) {
            errorMessage += "Автор книги не может быть пустым!\n";
        }
        if (yearField.getText() == null || yearField.getText().isEmpty()) {
            errorMessage += "Год не может быть пустым!\n";
        } else {
            try {
                Integer.parseInt(yearField.getText());
            } catch (NumberFormatException e) {
                errorMessage += "Год должен быть числом!\n";
            }
        }
        if (pagesField.getText() == null || pagesField.getText().isEmpty()) {
            errorMessage += "Количество страниц не может быть пустым!\n";
        } else {
            try {
                Integer.parseInt(pagesField.getText());
            } catch (NumberFormatException e) {
                errorMessage += "Количество страниц должно быть числом!\n";
            }
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            showAlert(Alert.AlertType.ERROR, "Неверный ввод", errorMessage);
            return false;
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Метод для обработки экспорта данных.
     */
    @FXML
    private void handleExport() {
        String selectedFormat = exportFormatComboBox.getValue();
        if (selectedFormat == null) {
            showAlert(Alert.AlertType.WARNING, "Предупреждение", "Пожалуйста, выберите формат экспорта.");
            return;
        }

        // В текущем случае поддерживается только CSV
        if (selectedFormat.equalsIgnoreCase("CSV")) {
            exportToCSV();
        } else {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Выбранный формат не поддерживается.");
        }
    }

    /**
     * Метод для экспорта данных в CSV файл.
     */
    private void exportToCSV() {
        // Используем FileChooser для выбора места сохранения файла
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Сохранить как");
        // Устанавливаем расширение файла по умолчанию
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        // Устанавливаем начальное имя файла
        fileChooser.setInitialFileName("books_export.csv");
        // Открываем диалог сохранения файла
        File file = fileChooser.showSaveDialog(TableView.getScene().getWindow());

        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                // Записываем заголовки
                writer.write("ID,Title,Author,Year,Pages");
                writer.newLine();

                // Записываем данные
                for (Books book : masterData) {
                    String line = String.format("%d,\"%s\",\"%s\",%d,%d",
                            book.getId(),
                            escapeSpecialCharacters(book.getTitle()),
                            escapeSpecialCharacters(book.getAuthor()),
                            book.getYear(),
                            book.getPages());
                    writer.write(line);
                    writer.newLine();
                }

                showAlert(Alert.AlertType.INFORMATION, "Успех", "Данные успешно экспортированы в CSV файл.");
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось сохранить CSV файл.");
                e.printStackTrace();
            }
        }
    }

    private String escapeSpecialCharacters(String data) {
        String escapedData = data;
        if (data.contains("\"")) {
            escapedData = data.replace("\"", "\"\"");
        }
        if (data.contains(",") || data.contains("\"") || data.contains("\n")) {
            escapedData = "\"" + escapedData + "\"";
        }
        return escapedData;
    }

    private void setupExportFormatComboBox() {
        exportFormatComboBox.setItems(FXCollections.observableArrayList("CSV"));
        exportFormatComboBox.setValue("CSV");
    }
}
