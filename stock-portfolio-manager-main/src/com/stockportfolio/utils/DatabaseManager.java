package com.stockportfolio.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.stockportfolio.model.Competition;
import com.stockportfolio.model.LeaderboardEntry;
import com.stockportfolio.model.Portfolio;
import com.stockportfolio.model.PortfolioLike;
import com.stockportfolio.model.Position;
import com.stockportfolio.model.SharedPortfolio;
import com.stockportfolio.model.Stock;
import com.stockportfolio.model.Transaction;
import com.stockportfolio.model.User;
import com.stockportfolio.model.WatchlistItem;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:h2:./data/stockportfolio;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;FILE_LOCK=NO";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private static DatabaseManager instance;
    private DataSource dataSource;

    public DataSource getDataSource() {
        return dataSource;
    }
    private Cache<String, Object> cache;

    private DatabaseManager() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        this.dataSource = new HikariDataSource(config);
        this.cache = CacheBuilder.newBuilder().maximumSize(1000).build();

        // Add shutdown hook to ensure database is closed on JVM exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                close();
            } catch (Exception e) {
                System.err.println("Error closing database on shutdown: " + e.getMessage());
            }
        }));

        initializeDatabase();
    }

    public static synchronized DatabaseManager getInstance() throws SQLException {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializeDatabase() throws SQLException {
        createTables();
        createDemoUser();
    }

    private void createTables() throws SQLException {
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "username VARCHAR(50) UNIQUE NOT NULL," +
                "email VARCHAR(100) UNIQUE NOT NULL," +
                "hashed_password VARCHAR(256) NOT NULL," +
                "salt VARCHAR(64) NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "last_login TIMESTAMP," +
                "is_active BOOLEAN DEFAULT TRUE," +
                "role VARCHAR(20) DEFAULT 'basic'," +
                "experience_points INT DEFAULT 0," +
                "level INT DEFAULT 1," +
                "trading_streak INT DEFAULT 0," +
                "total_pnl DOUBLE DEFAULT 0.0," +
                "trades_count INT DEFAULT 0," +
                "failed_login_attempts INT DEFAULT 0," +
                "account_locked_until TIMESTAMP," +
                "avatar_path VARCHAR(255)," +
                "bio TEXT," +
                "location VARCHAR(100)," +
                "website VARCHAR(255)" +
                ")";

        String createPortfoliosTable = "CREATE TABLE IF NOT EXISTS portfolios (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "user_id INT NOT NULL," +
                "name VARCHAR(100) NOT NULL," +
                "description TEXT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "total_value DOUBLE DEFAULT 0.0," +
                "total_cost_basis DOUBLE DEFAULT 0.0," +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ")";

        String createStocksTable = "CREATE TABLE IF NOT EXISTS stocks (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "symbol VARCHAR(10) UNIQUE NOT NULL," +
                "name VARCHAR(100)," +
                "exchange VARCHAR(20)," +
                "sector VARCHAR(50)," +
                "current_price DOUBLE," +
                "previous_close DOUBLE," +
                "change_value DOUBLE," +
                "change_percent DOUBLE," +
                "volume BIGINT," +
                "market_cap BIGINT," +
                "last_updated TIMESTAMP" +
                ")";

        String createPositionsTable = "CREATE TABLE IF NOT EXISTS positions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "portfolio_id INT NOT NULL," +
                "symbol VARCHAR(10) NOT NULL," +
                "asset_type VARCHAR(20) DEFAULT 'stock'," +
                "quantity DOUBLE NOT NULL," +
                "average_cost DOUBLE NOT NULL," +
                "current_price DOUBLE," +
                "total_value DOUBLE," +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (portfolio_id) REFERENCES portfolios(id)" +
                ")";

        String createTransactionsTable = "CREATE TABLE IF NOT EXISTS transactions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "user_id INT NOT NULL," +
                "portfolio_id INT NOT NULL," +
                "symbol VARCHAR(10) NOT NULL," +
                "type VARCHAR(10) NOT NULL," +
                "quantity DOUBLE NOT NULL," +
                "price DOUBLE NOT NULL," +
                "total_amount DOUBLE NOT NULL," +
                "order_type VARCHAR(20) DEFAULT 'MARKET'," +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "status VARCHAR(20) DEFAULT 'COMPLETED'," +
                "FOREIGN KEY (user_id) REFERENCES users(id)," +
                "FOREIGN KEY (portfolio_id) REFERENCES portfolios(id)" +
                ")";

        String createAlertsTable = "CREATE TABLE IF NOT EXISTS alerts (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "user_id INT NOT NULL," +
                "symbol VARCHAR(10)," +
                "alert_type VARCHAR(50) NOT NULL," +
                "threshold DOUBLE," +
                "condition VARCHAR(10)," +
                "is_active BOOLEAN DEFAULT TRUE," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "triggered_at TIMESTAMP," +
                "message TEXT," +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ")";

        String createFollowersTable = "CREATE TABLE IF NOT EXISTS followers (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "follower_id INT NOT NULL," +
                "followed_id INT NOT NULL," +
                "followed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (follower_id) REFERENCES users(id)," +
                "FOREIGN KEY (followed_id) REFERENCES users(id)," +
                "UNIQUE(follower_id, followed_id)" +
                ")";

        String createSharedPortfoliosTable = "CREATE TABLE IF NOT EXISTS shared_portfolios (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "portfolio_id INT NOT NULL," +
                "user_id INT NOT NULL," +
                "is_public BOOLEAN DEFAULT FALSE," +
                "shared_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "views INT DEFAULT 0," +
                "likes INT DEFAULT 0," +
                "FOREIGN KEY (portfolio_id) REFERENCES portfolios(id)," +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ")";

        String createStrategiesTable = "CREATE TABLE IF NOT EXISTS strategies (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "user_id INT NOT NULL," +
                "name VARCHAR(100) NOT NULL," +
                "description TEXT," +
                "strategy_type VARCHAR(50)," +
                "parameters TEXT," +
                "is_public BOOLEAN DEFAULT FALSE," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ")";

        String createCompetitionsTable = "CREATE TABLE IF NOT EXISTS competitions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "name VARCHAR(100) NOT NULL," +
                "description TEXT," +
                "start_date TIMESTAMP NOT NULL," +
                "end_date TIMESTAMP NOT NULL," +
                "rules TEXT," +
                "status VARCHAR(20) DEFAULT 'UPCOMING'," +
                "max_participants INT DEFAULT 100," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        String createLeaderboardEntriesTable = "CREATE TABLE IF NOT EXISTS leaderboard_entries (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "user_id INT NOT NULL," +
                "username VARCHAR(50) NOT NULL," +
                "total_return DOUBLE DEFAULT 0.0," +
                "total_value DOUBLE DEFAULT 0.0," +
                "rank INT NOT NULL," +
                "period VARCHAR(20) NOT NULL," +
                "calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ")";

        String createWatchlistTable = "CREATE TABLE IF NOT EXISTS watchlist (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "user_id INT NOT NULL," +
                "symbol VARCHAR(10) NOT NULL," +
                "added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "target_price DOUBLE," +
                "notes TEXT," +
                "FOREIGN KEY (user_id) REFERENCES users(id)," +
                "UNIQUE(user_id, symbol)" +
                ")";

        String createPortfolioLikesTable = "CREATE TABLE IF NOT EXISTS portfolio_likes (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "shared_portfolio_id INT NOT NULL," +
                "user_id INT NOT NULL," +
                "liked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (shared_portfolio_id) REFERENCES shared_portfolios(id)," +
                "FOREIGN KEY (user_id) REFERENCES users(id)," +
                "UNIQUE(shared_portfolio_id, user_id)" +
                ")";

        String createForumCategoriesTable = "CREATE TABLE IF NOT EXISTS forum_categories (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "name VARCHAR(100) NOT NULL UNIQUE," +
                "description TEXT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "post_count INT DEFAULT 0" +
                ")";

        String createForumPostsTable = "CREATE TABLE IF NOT EXISTS forum_posts (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "user_id INT NOT NULL," +
                "username VARCHAR(50) NOT NULL," +
                "category_id INT NOT NULL," +
                "title VARCHAR(200) NOT NULL," +
                "content TEXT NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "view_count INT DEFAULT 0," +
                "comment_count INT DEFAULT 0," +
                "is_pinned BOOLEAN DEFAULT FALSE," +
                "is_locked BOOLEAN DEFAULT FALSE," +
                "FOREIGN KEY (user_id) REFERENCES users(id)," +
                "FOREIGN KEY (category_id) REFERENCES forum_categories(id)" +
                ")";

        String createForumCommentsTable = "CREATE TABLE IF NOT EXISTS forum_comments (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "post_id INT NOT NULL," +
                "user_id INT NOT NULL," +
                "username VARCHAR(50) NOT NULL," +
                "content TEXT NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "parent_comment_id INT," +
                "FOREIGN KEY (post_id) REFERENCES forum_posts(id)," +
                "FOREIGN KEY (user_id) REFERENCES users(id)," +
                "FOREIGN KEY (parent_comment_id) REFERENCES forum_comments(id)" +
                ")";

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createPortfoliosTable);
            stmt.execute(createStocksTable);
            stmt.execute(createPositionsTable);
            stmt.execute(createTransactionsTable);
            stmt.execute(createAlertsTable);
            stmt.execute(createFollowersTable);
            stmt.execute(createSharedPortfoliosTable);
            stmt.execute(createStrategiesTable);
            stmt.execute(createCompetitionsTable);
            stmt.execute(createLeaderboardEntriesTable);
            stmt.execute(createWatchlistTable);
            stmt.execute(createPortfolioLikesTable);
            stmt.execute(createForumCategoriesTable);
            stmt.execute(createForumPostsTable);
            stmt.execute(createForumCommentsTable);

            // Create database indexes for performance optimization
            createIndexes(stmt);
        }
    }

    private void createIndexes(Statement stmt) throws SQLException {
        // Create indexes for frequently queried columns to improve performance

        // User indexes
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_users_is_active ON users(is_active)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_users_last_login ON users(last_login)");

        // Portfolio indexes
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_portfolios_user_id ON portfolios(user_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_portfolios_created_at ON portfolios(created_at)");

        // Stock indexes
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_stocks_symbol ON stocks(symbol)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_stocks_exchange ON stocks(exchange)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_stocks_sector ON stocks(sector)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_stocks_last_updated ON stocks(last_updated)");

        // Position indexes
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_positions_portfolio_id ON positions(portfolio_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_positions_symbol ON positions(symbol)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_positions_portfolio_symbol ON positions(portfolio_id, symbol)");

        // Transaction indexes
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions(user_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_portfolio_id ON transactions(portfolio_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_symbol ON transactions(symbol)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON transactions(timestamp)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_user_timestamp ON transactions(user_id, timestamp)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_portfolio_timestamp ON transactions(portfolio_id, timestamp)");

        // Alert indexes
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_alerts_user_id ON alerts(user_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_alerts_symbol ON alerts(symbol)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_alerts_is_active ON alerts(is_active)");

        // Follower indexes
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_followers_follower_id ON followers(follower_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_followers_followed_id ON followers(followed_id)");

        // Shared portfolio indexes
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_shared_portfolios_portfolio_id ON shared_portfolios(portfolio_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_shared_portfolios_user_id ON shared_portfolios(user_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_shared_portfolios_is_public ON shared_portfolios(is_public)");

        // Competition indexes
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_competitions_status ON competitions(status)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_competitions_start_date ON competitions(start_date)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_competitions_end_date ON competitions(end_date)");

        // Leaderboard indexes
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_leaderboard_entries_user_id ON leaderboard_entries(user_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_leaderboard_entries_period ON leaderboard_entries(period)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_leaderboard_entries_rank ON leaderboard_entries(rank)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_leaderboard_entries_period_rank ON leaderboard_entries(period, rank)");

        // Watchlist indexes
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_watchlist_user_id ON watchlist(user_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_watchlist_symbol ON watchlist(symbol)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_watchlist_user_symbol ON watchlist(user_id, symbol)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_watchlist_added_at ON watchlist(added_at)");

        // Forum indexes
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_forum_categories_name ON forum_categories(name)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_forum_posts_user_id ON forum_posts(user_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_forum_posts_category_id ON forum_posts(category_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_forum_posts_created_at ON forum_posts(created_at)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_forum_posts_is_pinned ON forum_posts(is_pinned)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_forum_comments_post_id ON forum_comments(post_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_forum_comments_user_id ON forum_comments(user_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_forum_comments_parent_id ON forum_comments(parent_comment_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_forum_comments_created_at ON forum_comments(created_at)");
    }

    // User CRUD
    public void saveUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, email, hashed_password, salt, created_at, is_active, role, experience_points, level, trading_streak, total_pnl, trades_count, failed_login_attempts, account_locked_until) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getHashedPassword());
            pstmt.setString(4, user.getSalt());
            pstmt.setTimestamp(5, Timestamp.valueOf(user.getCreatedAt()));
            pstmt.setBoolean(6, user.isActive());
            pstmt.setString(7, user.getRole());
            pstmt.setInt(8, user.getExperiencePoints());
            pstmt.setInt(9, user.getLevel());
            pstmt.setInt(10, user.getTradingStreak());
            pstmt.setDouble(11, user.getTotalPnL());
            pstmt.setInt(12, user.getTradesCount());
            pstmt.setInt(13, user.getFailedLoginAttempts());
            if (user.getAccountLockedUntil() != null) {
                pstmt.setTimestamp(14, Timestamp.valueOf(user.getAccountLockedUntil()));
            } else {
                pstmt.setNull(14, java.sql.Types.TIMESTAMP);
            }
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                user.setId(rs.getInt(1));
            }
        }
    }

    public User getUserByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setHashedPassword(rs.getString("hashed_password"));
                user.setSalt(rs.getString("salt"));
                user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                if (rs.getTimestamp("last_login") != null) {
                    user.setLastLogin(rs.getTimestamp("last_login").toLocalDateTime());
                }
                user.setActive(rs.getBoolean("is_active"));
                user.setRole(rs.getString("role"));
                user.setExperiencePoints(rs.getInt("experience_points"));
                user.setLevel(rs.getInt("level"));
                user.setTradingStreak(rs.getInt("trading_streak"));
                user.setTotalPnL(rs.getDouble("total_pnl"));
                user.setTradesCount(rs.getInt("trades_count"));
                user.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
                if (rs.getTimestamp("account_locked_until") != null) {
                    user.setAccountLockedUntil(rs.getTimestamp("account_locked_until").toLocalDateTime());
                }
                return user;
            }
        }
        return null;
    }

    public void updateUserLoginSecurity(int userId, int failedAttempts, java.time.LocalDateTime lockUntil) throws SQLException {
        String sql = "UPDATE users SET failed_login_attempts = ?, account_locked_until = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, failedAttempts);
            if (lockUntil != null) {
                pstmt.setTimestamp(2, Timestamp.valueOf(lockUntil));
            } else {
                pstmt.setNull(2, java.sql.Types.TIMESTAMP);
            }
            pstmt.setInt(3, userId);
            pstmt.executeUpdate();
        }
    }

    public void updateLastLogin(int userId) throws SQLException {
        String sql = "UPDATE users SET last_login = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(java.time.LocalDateTime.now()));
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
    }

    public void updateUserInfo(int userId, String username, String email) throws SQLException {
        String sql = "UPDATE users SET username = ?, email = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setInt(3, userId);
            pstmt.executeUpdate();
        }
    }

    // Portfolio CRUD
    public void savePortfolio(Portfolio portfolio) throws SQLException {
        String sql = "INSERT INTO portfolios (user_id, name, description, created_at, updated_at, total_value, total_cost_basis) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, portfolio.getUserId());
            pstmt.setString(2, portfolio.getName());
            pstmt.setString(3, portfolio.getDescription());
            pstmt.setTimestamp(4, Timestamp.valueOf(portfolio.getCreatedAt()));
            pstmt.setTimestamp(5, Timestamp.valueOf(portfolio.getUpdatedAt()));
            pstmt.setDouble(6, portfolio.getTotalValue());
            pstmt.setDouble(7, portfolio.getTotalCostBasis());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                portfolio.setId(rs.getInt(1));
            }
        }
    }

    public Portfolio getPortfolioById(int id) throws SQLException {
        String sql = "SELECT * FROM portfolios WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Portfolio portfolio = new Portfolio();
                portfolio.setId(rs.getInt("id"));
                portfolio.setUserId(rs.getInt("user_id"));
                portfolio.setName(rs.getString("name"));
                portfolio.setDescription(rs.getString("description"));
                portfolio.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                portfolio.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                portfolio.setTotalValue(rs.getDouble("total_value"));
                portfolio.setTotalCostBasis(rs.getDouble("total_cost_basis"));
                return portfolio;
            }
        }
        return null;
    }

    public List<Portfolio> getPortfoliosByUserId(int userId) throws SQLException {
        List<Portfolio> portfolios = new ArrayList<>();
        String sql = "SELECT * FROM portfolios WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Portfolio portfolio = new Portfolio();
                portfolio.setId(rs.getInt("id"));
                portfolio.setUserId(rs.getInt("user_id"));
                portfolio.setName(rs.getString("name"));
                portfolio.setDescription(rs.getString("description"));
                portfolio.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                portfolio.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                portfolio.setTotalValue(rs.getDouble("total_value"));
                portfolio.setTotalCostBasis(rs.getDouble("total_cost_basis"));
                portfolios.add(portfolio);
            }
        }
        return portfolios;
    }

    public void updatePortfolio(Portfolio portfolio) throws SQLException {
        String sql = "UPDATE portfolios SET name = ?, description = ?, updated_at = ?, total_value = ?, total_cost_basis = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, portfolio.getName());
            pstmt.setString(2, portfolio.getDescription());
            pstmt.setTimestamp(3, Timestamp.valueOf(portfolio.getUpdatedAt()));
            pstmt.setDouble(4, portfolio.getTotalValue());
            pstmt.setDouble(5, portfolio.getTotalCostBasis());
            pstmt.setInt(6, portfolio.getId());
            pstmt.executeUpdate();
        }
    }

    public void deletePortfolio(int id) throws SQLException {
        String sql = "DELETE FROM portfolios WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    // Position CRUD
    public void savePosition(Position position) throws SQLException {
        String sql = "INSERT INTO positions (portfolio_id, symbol, asset_type, quantity, average_cost, current_price, total_value, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, position.getPortfolioId());
            pstmt.setString(2, position.getSymbol());
            pstmt.setString(3, position.getAssetType());
            pstmt.setDouble(4, position.getQuantity());
            pstmt.setDouble(5, position.getAverageCost());
            pstmt.setDouble(6, position.getCurrentPrice());
            pstmt.setDouble(7, position.getTotalValue());
            pstmt.setTimestamp(8, Timestamp.valueOf(position.getLastUpdated()));
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                position.setId(rs.getInt(1));
            }
        }
    }

    public Position getPositionById(int id) throws SQLException {
        String sql = "SELECT * FROM positions WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Position position = new Position();
                position.setId(rs.getInt("id"));
                position.setPortfolioId(rs.getInt("portfolio_id"));
                position.setSymbol(rs.getString("symbol"));
                position.setAssetType(rs.getString("asset_type"));
                position.setQuantity(rs.getDouble("quantity"));
                position.setAverageCost(rs.getDouble("average_cost"));
                position.setCurrentPrice(rs.getDouble("current_price"));
                position.setTotalValue(rs.getDouble("total_value"));
                position.setLastUpdated(rs.getTimestamp("last_updated").toLocalDateTime());
                return position;
            }
        }
        return null;
    }

    public List<Position> getPositionsByPortfolioId(int portfolioId) throws SQLException {
        List<Position> positions = new ArrayList<>();
        String sql = "SELECT * FROM positions WHERE portfolio_id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, portfolioId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Position position = new Position();
                position.setId(rs.getInt("id"));
                position.setPortfolioId(rs.getInt("portfolio_id"));
                position.setSymbol(rs.getString("symbol"));
                position.setAssetType(rs.getString("asset_type"));
                position.setQuantity(rs.getDouble("quantity"));
                position.setAverageCost(rs.getDouble("average_cost"));
                position.setCurrentPrice(rs.getDouble("current_price"));
                position.setTotalValue(rs.getDouble("total_value"));
                position.setLastUpdated(rs.getTimestamp("last_updated").toLocalDateTime());
                positions.add(position);
            }
        }
        return positions;
    }

    public void updatePosition(Position position) throws SQLException {
        String sql = "UPDATE positions SET quantity = ?, average_cost = ?, current_price = ?, total_value = ?, last_updated = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, position.getQuantity());
            pstmt.setDouble(2, position.getAverageCost());
            pstmt.setDouble(3, position.getCurrentPrice());
            pstmt.setDouble(4, position.getTotalValue());
            pstmt.setTimestamp(5, Timestamp.valueOf(position.getLastUpdated()));
            pstmt.setInt(6, position.getId());
            pstmt.executeUpdate();
        }
    }

    public void deletePosition(int id) throws SQLException {
        String sql = "DELETE FROM positions WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    // Transaction CRUD
    public void saveTransaction(Transaction transaction) throws SQLException {
        String sql = "INSERT INTO transactions (user_id, portfolio_id, symbol, type, quantity, price, total_amount, order_type, timestamp, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, transaction.getUserId());
            pstmt.setInt(2, transaction.getPortfolioId());
            pstmt.setString(3, transaction.getStockSymbol());
            pstmt.setString(4, transaction.getType());
            pstmt.setDouble(5, transaction.getQuantity());
            pstmt.setDouble(6, transaction.getPrice());
            pstmt.setDouble(7, transaction.getTotalAmount());
            pstmt.setString(8, transaction.getOrderType());
            pstmt.setTimestamp(9, Timestamp.valueOf(transaction.getTimestamp()));
            pstmt.setString(10, transaction.getStatus());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                transaction.setId(rs.getInt(1));
            }
        }
    }

    public Transaction getTransactionById(int id) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.setId(rs.getInt("id"));
                transaction.setUserId(rs.getInt("user_id"));
                transaction.setPortfolioId(rs.getInt("portfolio_id"));
                transaction.setStockSymbol(rs.getString("symbol"));
                transaction.setType(rs.getString("type"));
                transaction.setQuantity(rs.getDouble("quantity"));
                transaction.setPrice(rs.getDouble("price"));
                transaction.setTotalAmount(rs.getDouble("total_amount"));
                transaction.setOrderType(rs.getString("order_type"));
                transaction.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
                transaction.setStatus(rs.getString("status"));
                return transaction;
            }
        }
        return null;
    }

    public List<Transaction> getTransactionsByUserId(int userId) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.setId(rs.getInt("id"));
                transaction.setUserId(rs.getInt("user_id"));
                transaction.setPortfolioId(rs.getInt("portfolio_id"));
                transaction.setSymbol(rs.getString("symbol"));
                transaction.setType(rs.getString("type"));
                transaction.setQuantity(rs.getDouble("quantity"));
                transaction.setPrice(rs.getDouble("price"));
                transaction.setTotalAmount(rs.getDouble("total_amount"));
                transaction.setOrderType(rs.getString("order_type"));
                transaction.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
                transaction.setStatus(rs.getString("status"));
                transactions.add(transaction);
            }
        }
        return transactions;
    }

    public List<Transaction> getTransactionsByPortfolioId(int portfolioId) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE portfolio_id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, portfolioId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.setId(rs.getInt("id"));
                transaction.setUserId(rs.getInt("user_id"));
                transaction.setPortfolioId(rs.getInt("portfolio_id"));
                transaction.setSymbol(rs.getString("symbol"));
                transaction.setType(rs.getString("type"));
                transaction.setQuantity(rs.getDouble("quantity"));
                transaction.setPrice(rs.getDouble("price"));
                transaction.setTotalAmount(rs.getDouble("total_amount"));
                transaction.setOrderType(rs.getString("order_type"));
                transaction.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
                transaction.setStatus(rs.getString("status"));
                transactions.add(transaction);
            }
        }
        return transactions;
    }

    public void updateTransaction(Transaction transaction) throws SQLException {
        String sql = "UPDATE transactions SET status = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, transaction.getStatus());
            pstmt.setInt(2, transaction.getId());
            pstmt.executeUpdate();
        }
    }

    public void deleteTransaction(int id) throws SQLException {
        String sql = "DELETE FROM transactions WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    // Stock CRUD
    public void saveStock(Stock stock) throws SQLException {
        String sql = "INSERT INTO stocks (symbol, name, exchange, sector, current_price, previous_close, change_value, change_percent, volume, market_cap, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, stock.getSymbol());
            pstmt.setString(2, stock.getName());
            pstmt.setString(3, stock.getExchange());
            pstmt.setString(4, stock.getSector());
            pstmt.setDouble(5, stock.getCurrentPrice());
            pstmt.setDouble(6, stock.getPreviousClose());
            pstmt.setDouble(7, stock.getChange());
            pstmt.setDouble(8, stock.getChangePercent());
            pstmt.setLong(9, stock.getVolume());
            pstmt.setLong(10, stock.getMarketCap());
            pstmt.setTimestamp(11, Timestamp.valueOf(stock.getLastUpdated()));
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                stock.setId(rs.getInt(1));
            }
        }
    }

    public Stock getStockBySymbol(String symbol) throws SQLException {
        String sql = "SELECT * FROM stocks WHERE symbol = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, symbol);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Stock stock = new Stock();
                stock.setId(rs.getInt("id"));
                stock.setSymbol(rs.getString("symbol"));
                stock.setName(rs.getString("name"));
                stock.setExchange(rs.getString("exchange"));
                stock.setSector(rs.getString("sector"));
                stock.setCurrentPrice(rs.getDouble("current_price"));
                stock.setPreviousClose(rs.getDouble("previous_close"));
                stock.setChange(rs.getDouble("change_value"));
                stock.setChangePercent(rs.getDouble("change_percent"));
                stock.setVolume(rs.getLong("volume"));
                stock.setMarketCap(rs.getLong("market_cap"));
                stock.setLastUpdated(rs.getTimestamp("last_updated").toLocalDateTime());
                return stock;
            }
        }
        return null;
    }

    public List<Stock> getAllStocks() throws SQLException {
        List<Stock> stocks = new ArrayList<>();
        String sql = "SELECT * FROM stocks";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Stock stock = new Stock();
                stock.setId(rs.getInt("id"));
                stock.setSymbol(rs.getString("symbol"));
                stock.setName(rs.getString("name"));
                stock.setExchange(rs.getString("exchange"));
                stock.setSector(rs.getString("sector"));
                stock.setCurrentPrice(rs.getDouble("current_price"));
                stock.setPreviousClose(rs.getDouble("previous_close"));
                stock.setChange(rs.getDouble("change_value"));
                stock.setChangePercent(rs.getDouble("change_percent"));
                stock.setVolume(rs.getLong("volume"));
                stock.setMarketCap(rs.getLong("market_cap"));
                stock.setLastUpdated(rs.getTimestamp("last_updated").toLocalDateTime());
                stocks.add(stock);
            }
        }
        return stocks;
    }

    public void updateStock(Stock stock) throws SQLException {
        String sql = "UPDATE stocks SET name = ?, exchange = ?, sector = ?, current_price = ?, previous_close = ?, change_value = ?, change_percent = ?, volume = ?, market_cap = ?, last_updated = ? WHERE symbol = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, stock.getName());
            pstmt.setString(2, stock.getExchange());
            pstmt.setString(3, stock.getSector());
            pstmt.setDouble(4, stock.getCurrentPrice());
            pstmt.setDouble(5, stock.getPreviousClose());
            pstmt.setDouble(6, stock.getChange());
            pstmt.setDouble(7, stock.getChangePercent());
            pstmt.setLong(8, stock.getVolume());
            pstmt.setLong(9, stock.getMarketCap());
            pstmt.setTimestamp(10, Timestamp.valueOf(stock.getLastUpdated()));
            pstmt.setString(11, stock.getSymbol());
            pstmt.executeUpdate();
        }
    }

    public void deleteStock(String symbol) throws SQLException {
        String sql = "DELETE FROM stocks WHERE symbol = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, symbol);
            pstmt.executeUpdate();
        }
    }



    public void close() throws SQLException {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
        }
    }

    // Additional CRUD methods can be added here

    private void createDemoUser() {
        try {
            System.out.println("Creating demo data...");
            // Populate stocks table with Fortune 500/blue-chip stocks first
            populateFortune500Stocks();

            // Check if demo user already exists
            User existingUser = getUserByUsername("demo");
            if (existingUser != null) {
                System.out.println("Demo user already exists.");
                return; // Demo user already exists
            }

            // Create demo user with username "demo" and password "demo123"
            String salt = PasswordSecurity.generateSalt();
            String hashedPassword = PasswordSecurity.hashPassword("demo123", salt);

            User demoUser = new User();
            demoUser.setUsername("demo");
            demoUser.setEmail("demo@example.com");
            demoUser.setSalt(salt);
            demoUser.setHashedPassword(hashedPassword);
            demoUser.setActive(true);
            demoUser.setRole("basic");
            demoUser.setExperiencePoints(0);
            demoUser.setLevel(1);
            demoUser.setTradingStreak(0);
            demoUser.setTotalPnL(0.0);
            demoUser.setTradesCount(0);
            demoUser.setFailedLoginAttempts(0);
            demoUser.setAccountLockedUntil(null);
            demoUser.setCreatedAt(java.time.LocalDateTime.now());

            saveUser(demoUser);
            System.out.println("Demo user created with ID: " + demoUser.getId());

            // Create demo portfolio for demo user
            Portfolio demoPortfolio = new Portfolio();
            demoPortfolio.setUserId(demoUser.getId());
            demoPortfolio.setName("Demo Portfolio");
            demoPortfolio.setDescription("This is a demo portfolio.");
            demoPortfolio.setCreatedAt(java.time.LocalDateTime.now());
            demoPortfolio.setUpdatedAt(java.time.LocalDateTime.now());
            demoPortfolio.setTotalValue(0.0);
            demoPortfolio.setTotalCostBasis(0.0);

            savePortfolio(demoPortfolio);
            System.out.println("Demo portfolio created with ID: " + demoPortfolio.getId());



            System.out.println("Demo data creation completed.");

            // Create sample competition data
            createSampleCompetitionData();

            // Create sample leaderboard data
            createSampleLeaderboardData();

        } catch (Exception e) {
            System.err.println("Error creating demo user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createSampleCompetitionData() {
        try {
            System.out.println("Creating sample competition data...");

            // Check if sample competition already exists
            Competition existingCompetition = getCompetitionByName("Monthly Trading Challenge");
            if (existingCompetition != null) {
                System.out.println("Sample competition already exists.");
                return;
            }

            // Create sample competition
            Competition competition = new Competition();
            competition.setName("Monthly Trading Challenge");
            competition.setDescription("Compete for the highest portfolio returns this month");
            competition.setStartDate(java.time.LocalDateTime.of(2024, 1, 1, 0, 0));
            competition.setEndDate(java.time.LocalDateTime.of(2024, 1, 31, 23, 59));
            competition.setRules("No restrictions on trading. Winner gets bragging rights!");
            competition.setStatus("ACTIVE");
            competition.setMaxParticipants(100);
            competition.setCreatedAt(java.time.LocalDateTime.now());

            // Insert competition
            String sql = "INSERT INTO competitions (name, description, start_date, end_date, rules, status, max_participants, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, competition.getName());
                pstmt.setString(2, competition.getDescription());
                pstmt.setTimestamp(3, Timestamp.valueOf(competition.getStartDate()));
                pstmt.setTimestamp(4, Timestamp.valueOf(competition.getEndDate()));
                pstmt.setString(5, competition.getRules());
                pstmt.setString(6, competition.getStatus());
                pstmt.setInt(7, competition.getMaxParticipants());
                pstmt.setTimestamp(8, Timestamp.valueOf(competition.getCreatedAt()));
                pstmt.executeUpdate();
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    competition.setId(rs.getInt(1));
                }
            }

            System.out.println("Sample competition created with ID: " + competition.getId());

        } catch (Exception e) {
            System.err.println("Error creating sample competition data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createSampleLeaderboardData() {
        try {
            System.out.println("Creating sample leaderboard data...");

            // Check if sample leaderboard entries already exist
            List<LeaderboardEntry> existingEntries = getLeaderboardEntries("MONTHLY");
            if (!existingEntries.isEmpty()) {
                System.out.println("Sample leaderboard data already exists.");
                return;
            }

            // Create sample leaderboard entries
            LeaderboardEntry entry1 = new LeaderboardEntry();
            entry1.setUserId(1); // Assuming demo user has ID 1
            entry1.setUsername("trader1");
            entry1.setTotalReturn(15.5);
            entry1.setTotalValue(11550.0);
            entry1.setRank(1);
            entry1.setPeriod("MONTHLY");
            entry1.setCalculatedAt(java.time.LocalDateTime.now());

            LeaderboardEntry entry2 = new LeaderboardEntry();
            entry2.setUserId(1); // Using same user for demo
            entry2.setUsername("trader2");
            entry2.setTotalReturn(12.3);
            entry2.setTotalValue(11230.0);
            entry2.setRank(2);
            entry2.setPeriod("MONTHLY");
            entry2.setCalculatedAt(java.time.LocalDateTime.now());

            LeaderboardEntry entry3 = new LeaderboardEntry();
            entry3.setUserId(1); // Using same user for demo
            entry3.setUsername("trader3");
            entry3.setTotalReturn(10.8);
            entry3.setTotalValue(11080.0);
            entry3.setRank(3);
            entry3.setPeriod("MONTHLY");
            entry3.setCalculatedAt(java.time.LocalDateTime.now());

            // Insert leaderboard entries
            String sql = "INSERT INTO leaderboard_entries (user_id, username, total_return, total_value, rank, period, calculated_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
            LeaderboardEntry[] entries = {entry1, entry2, entry3};

            for (LeaderboardEntry entry : entries) {
                try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setInt(1, entry.getUserId());
                    pstmt.setString(2, entry.getUsername());
                    pstmt.setDouble(3, entry.getTotalReturn());
                    pstmt.setDouble(4, entry.getTotalValue());
                    pstmt.setInt(5, entry.getRank());
                    pstmt.setString(6, entry.getPeriod());
                    pstmt.setTimestamp(7, Timestamp.valueOf(entry.getCalculatedAt()));
                    pstmt.executeUpdate();
                    ResultSet rs = pstmt.getGeneratedKeys();
                    if (rs.next()) {
                        entry.setId(rs.getInt(1));
                    }
                }
            }

            System.out.println("Sample leaderboard entries created.");

        } catch (Exception e) {
            System.err.println("Error creating sample leaderboard data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Community methods
    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setHashedPassword(rs.getString("hashed_password"));
                user.setSalt(rs.getString("salt"));
                user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                if (rs.getTimestamp("last_login") != null) {
                    user.setLastLogin(rs.getTimestamp("last_login").toLocalDateTime());
                }
                user.setActive(rs.getBoolean("is_active"));
                user.setRole(rs.getString("role"));
                user.setExperiencePoints(rs.getInt("experience_points"));
                user.setLevel(rs.getInt("level"));
                user.setTradingStreak(rs.getInt("trading_streak"));
                user.setTotalPnL(rs.getDouble("total_pnl"));
                user.setTradesCount(rs.getInt("trades_count"));
                user.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
                if (rs.getTimestamp("account_locked_until") != null) {
                    user.setAccountLockedUntil(rs.getTimestamp("account_locked_until").toLocalDateTime());
                }
                user.setAvatarPath(rs.getString("avatar_path"));
                user.setBio(rs.getString("bio"));
                user.setLocation(rs.getString("location"));
                user.setWebsite(rs.getString("website"));
                users.add(user);
            }
        }
        return users;
    }

    public void followUser(int followerId, int followedId) throws SQLException {
        String sql = "INSERT INTO followers (follower_id, followed_id) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, followerId);
            pstmt.setInt(2, followedId);
            pstmt.executeUpdate();
        }
    }

    public void unfollowUser(int followerId, int followedId) throws SQLException {
        String sql = "DELETE FROM followers WHERE follower_id = ? AND followed_id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, followerId);
            pstmt.setInt(2, followedId);
            pstmt.executeUpdate();
        }
    }

    // Competition methods
    public List<Competition> getActiveCompetitions() throws SQLException {
        List<Competition> competitions = new ArrayList<>();
        String sql = "SELECT * FROM competitions WHERE status = 'ACTIVE'";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Competition competition = new Competition();
                competition.setId(rs.getInt("id"));
                competition.setName(rs.getString("name"));
                competition.setDescription(rs.getString("description"));
                competition.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
                competition.setEndDate(rs.getTimestamp("end_date").toLocalDateTime());
                competition.setRules(rs.getString("rules"));
                competition.setStatus(rs.getString("status"));
                competition.setMaxParticipants(rs.getInt("max_participants"));
                competition.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                competitions.add(competition);
            }
        }
        return competitions;
    }

    public Competition getCompetitionByName(String name) throws SQLException {
        String sql = "SELECT * FROM competitions WHERE name = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Competition competition = new Competition();
                competition.setId(rs.getInt("id"));
                competition.setName(rs.getString("name"));
                competition.setDescription(rs.getString("description"));
                competition.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
                competition.setEndDate(rs.getTimestamp("end_date").toLocalDateTime());
                competition.setRules(rs.getString("rules"));
                competition.setStatus(rs.getString("status"));
                competition.setMaxParticipants(rs.getInt("max_participants"));
                competition.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                return competition;
            }
        }
        return null;
    }

    public void joinCompetition(int userId, int competitionId) throws SQLException {
        // Assuming there's a competition_participants table, but since it's not defined, I'll skip for now
        // This needs to be implemented based on the actual table structure
        throw new UnsupportedOperationException("joinCompetition not implemented yet");
    }

    public List<LeaderboardEntry> getCompetitionLeaderboard(int competitionId) throws SQLException {
        List<LeaderboardEntry> entries = new ArrayList<>();
        // This is a placeholder - actual implementation would depend on how leaderboard is calculated
        String sql = "SELECT * FROM leaderboard_entries WHERE period = 'competition' ORDER BY rank ASC";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                LeaderboardEntry entry = new LeaderboardEntry();
                entry.setId(rs.getInt("id"));
                entry.setUserId(rs.getInt("user_id"));
                entry.setUsername(rs.getString("username"));
                entry.setTotalReturn(rs.getDouble("total_return"));
                entry.setTotalValue(rs.getDouble("total_value"));
                entry.setRank(rs.getInt("rank"));
                entry.setPeriod(rs.getString("period"));
                entry.setCalculatedAt(rs.getTimestamp("calculated_at").toLocalDateTime());
                entries.add(entry);
            }
        }
        return entries;
    }

    // Leaderboard methods
    public List<LeaderboardEntry> getLeaderboardEntries(String period) throws SQLException {
        List<LeaderboardEntry> entries = new ArrayList<>();
        String sql = "SELECT * FROM leaderboard_entries WHERE period = ? ORDER BY rank ASC";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, period);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                LeaderboardEntry entry = new LeaderboardEntry();
                entry.setId(rs.getInt("id"));
                entry.setUserId(rs.getInt("user_id"));
                entry.setUsername(rs.getString("username"));
                entry.setTotalReturn(rs.getDouble("total_return"));
                entry.setTotalValue(rs.getDouble("total_value"));
                entry.setRank(rs.getInt("rank"));
                entry.setPeriod(rs.getString("period"));
                entry.setCalculatedAt(rs.getTimestamp("calculated_at").toLocalDateTime());
                entries.add(entry);
            }
        }
        return entries;
    }

    // Watchlist CRUD methods
    public void saveWatchlistItem(WatchlistItem item) throws SQLException {
        String sql = "INSERT INTO watchlist (user_id, symbol, added_at, target_price, notes) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, item.getUserId());
            pstmt.setString(2, item.getSymbol());
            pstmt.setTimestamp(3, Timestamp.valueOf(item.getAddedAt()));
            if (item.getTargetPrice() != 0.0) {
                pstmt.setDouble(4, item.getTargetPrice());
            } else {
                pstmt.setNull(4, java.sql.Types.DOUBLE);
            }
            pstmt.setString(5, item.getNotes());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                item.setId(rs.getInt(1));
            }
        }
    }

    public WatchlistItem getWatchlistItemById(int id) throws SQLException {
        String sql = "SELECT * FROM watchlist WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                WatchlistItem item = new WatchlistItem();
                item.setId(rs.getInt("id"));
                item.setUserId(rs.getInt("user_id"));
                item.setSymbol(rs.getString("symbol"));
                item.setAddedAt(rs.getTimestamp("added_at").toLocalDateTime());
                item.setTargetPrice(rs.getDouble("target_price"));
                item.setNotes(rs.getString("notes"));
                return item;
            }
        }
        return null;
    }

    public List<WatchlistItem> getWatchlistByUserId(int userId) throws SQLException {
        List<WatchlistItem> items = new ArrayList<>();
        String sql = "SELECT * FROM watchlist WHERE user_id = ? ORDER BY added_at DESC";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                WatchlistItem item = new WatchlistItem();
                item.setId(rs.getInt("id"));
                item.setUserId(rs.getInt("user_id"));
                item.setSymbol(rs.getString("symbol"));
                item.setAddedAt(rs.getTimestamp("added_at").toLocalDateTime());
                item.setTargetPrice(rs.getDouble("target_price"));
                item.setNotes(rs.getString("notes"));
                items.add(item);
            }
        }
        return items;
    }

    public void updateWatchlistItem(WatchlistItem item) throws SQLException {
        String sql = "UPDATE watchlist SET target_price = ?, notes = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (item.getTargetPrice() != 0.0) {
                pstmt.setDouble(1, item.getTargetPrice());
            } else {
                pstmt.setNull(1, java.sql.Types.DOUBLE);
            }
            pstmt.setString(2, item.getNotes());
            pstmt.setInt(3, item.getId());
            pstmt.executeUpdate();
        }
    }

    public void deleteWatchlistItem(int id) throws SQLException {
        String sql = "DELETE FROM watchlist WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public boolean isSymbolInWatchlist(int userId, String symbol) throws SQLException {
        String sql = "SELECT COUNT(*) FROM watchlist WHERE user_id = ? AND symbol = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, symbol);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    // Portfolio Sharing methods
    public void sharePortfolio(SharedPortfolio sharedPortfolio) throws SQLException {
        String sql = "INSERT INTO shared_portfolios (portfolio_id, user_id, is_public, shared_at, views, likes) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, sharedPortfolio.getPortfolioId());
            pstmt.setInt(2, sharedPortfolio.getUserId());
            pstmt.setBoolean(3, sharedPortfolio.isPublic());
            pstmt.setTimestamp(4, Timestamp.valueOf(sharedPortfolio.getSharedAt()));
            pstmt.setInt(5, sharedPortfolio.getViews());
            pstmt.setInt(6, sharedPortfolio.getLikes());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                sharedPortfolio.setId(rs.getInt(1));
            }
        }
    }

    public SharedPortfolio getSharedPortfolioById(int id) throws SQLException {
        String sql = "SELECT * FROM shared_portfolios WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                SharedPortfolio sharedPortfolio = new SharedPortfolio();
                sharedPortfolio.setId(rs.getInt("id"));
                sharedPortfolio.setPortfolioId(rs.getInt("portfolio_id"));
                sharedPortfolio.setUserId(rs.getInt("user_id"));
                sharedPortfolio.setPublic(rs.getBoolean("is_public"));
                sharedPortfolio.setSharedAt(rs.getTimestamp("shared_at").toLocalDateTime());
                sharedPortfolio.setViews(rs.getInt("views"));
                sharedPortfolio.setLikes(rs.getInt("likes"));
                return sharedPortfolio;
            }
        }
        return null;
    }

    public List<SharedPortfolio> getPublicSharedPortfolios() throws SQLException {
        List<SharedPortfolio> sharedPortfolios = new ArrayList<>();
        String sql = "SELECT * FROM shared_portfolios WHERE is_public = true ORDER BY shared_at DESC";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                SharedPortfolio sharedPortfolio = new SharedPortfolio();
                sharedPortfolio.setId(rs.getInt("id"));
                sharedPortfolio.setPortfolioId(rs.getInt("portfolio_id"));
                sharedPortfolio.setUserId(rs.getInt("user_id"));
                sharedPortfolio.setPublic(rs.getBoolean("is_public"));
                sharedPortfolio.setSharedAt(rs.getTimestamp("shared_at").toLocalDateTime());
                sharedPortfolio.setViews(rs.getInt("views"));
                sharedPortfolio.setLikes(rs.getInt("likes"));
                sharedPortfolios.add(sharedPortfolio);
            }
        }
        return sharedPortfolios;
    }

    public List<SharedPortfolio> getSharedPortfoliosByUserId(int userId) throws SQLException {
        List<SharedPortfolio> sharedPortfolios = new ArrayList<>();
        String sql = "SELECT * FROM shared_portfolios WHERE user_id = ? ORDER BY shared_at DESC";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                SharedPortfolio sharedPortfolio = new SharedPortfolio();
                sharedPortfolio.setId(rs.getInt("id"));
                sharedPortfolio.setPortfolioId(rs.getInt("portfolio_id"));
                sharedPortfolio.setUserId(rs.getInt("user_id"));
                sharedPortfolio.setPublic(rs.getBoolean("is_public"));
                sharedPortfolio.setSharedAt(rs.getTimestamp("shared_at").toLocalDateTime());
                sharedPortfolio.setViews(rs.getInt("views"));
                sharedPortfolio.setLikes(rs.getInt("likes"));
                sharedPortfolios.add(sharedPortfolio);
            }
        }
        return sharedPortfolios;
    }

    public void incrementPortfolioViews(int sharedPortfolioId) throws SQLException {
        String sql = "UPDATE shared_portfolios SET views = views + 1 WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sharedPortfolioId);
            pstmt.executeUpdate();
        }
    }

    public void incrementPortfolioLikes(int sharedPortfolioId) throws SQLException {
        String sql = "UPDATE shared_portfolios SET likes = likes + 1 WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sharedPortfolioId);
            pstmt.executeUpdate();
        }
    }

    public void decrementPortfolioLikes(int sharedPortfolioId) throws SQLException {
        String sql = "UPDATE shared_portfolios SET likes = GREATEST(likes - 1, 0) WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sharedPortfolioId);
            pstmt.executeUpdate();
        }
    }

    public boolean isPortfolioShared(int portfolioId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM shared_portfolios WHERE portfolio_id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, portfolioId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public void updateSharedPortfolio(SharedPortfolio sharedPortfolio) throws SQLException {
        String sql = "UPDATE shared_portfolios SET is_public = ?, views = ?, likes = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, sharedPortfolio.isPublic());
            pstmt.setInt(2, sharedPortfolio.getViews());
            pstmt.setInt(3, sharedPortfolio.getLikes());
            pstmt.setInt(4, sharedPortfolio.getId());
            pstmt.executeUpdate();
        }
    }

    public void deleteSharedPortfolio(int sharedPortfolioId) throws SQLException {
        String sql = "DELETE FROM shared_portfolios WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sharedPortfolioId);
            pstmt.executeUpdate();
        }
    }

    // Portfolio Like methods
    public void likePortfolio(PortfolioLike portfolioLike) throws SQLException {
        String sql = "INSERT INTO portfolio_likes (shared_portfolio_id, user_id, liked_at) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, portfolioLike.getSharedPortfolioId());
            pstmt.setInt(2, portfolioLike.getUserId());
            pstmt.setTimestamp(3, Timestamp.valueOf(portfolioLike.getLikedAt()));
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                portfolioLike.setId(rs.getInt(1));
            }
        }
    }

    public void unlikePortfolio(int sharedPortfolioId, int userId) throws SQLException {
        String sql = "DELETE FROM portfolio_likes WHERE shared_portfolio_id = ? AND user_id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sharedPortfolioId);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
    }

    public boolean hasUserLikedPortfolio(int sharedPortfolioId, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM portfolio_likes WHERE shared_portfolio_id = ? AND user_id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sharedPortfolioId);
            pstmt.setInt(2, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public List<PortfolioLike> getPortfolioLikes(int sharedPortfolioId) throws SQLException {
        List<PortfolioLike> likes = new ArrayList<>();
        String sql = "SELECT * FROM portfolio_likes WHERE shared_portfolio_id = ? ORDER BY liked_at DESC";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sharedPortfolioId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                PortfolioLike like = new PortfolioLike();
                like.setId(rs.getInt("id"));
                like.setSharedPortfolioId(rs.getInt("shared_portfolio_id"));
                like.setUserId(rs.getInt("user_id"));
                like.setLikedAt(rs.getTimestamp("liked_at").toLocalDateTime());
                likes.add(like);
            }
        }
        return likes;
    }

    public int getPortfolioLikeCount(int sharedPortfolioId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM portfolio_likes WHERE shared_portfolio_id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sharedPortfolioId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    // Forum Category CRUD methods
    public void saveForumCategory(com.stockportfolio.model.ForumCategory category) throws SQLException {
        String sql = "INSERT INTO forum_categories (name, description, created_at, post_count) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, category.getName());
            pstmt.setString(2, category.getDescription());
            pstmt.setTimestamp(3, Timestamp.valueOf(category.getCreatedAt()));
            pstmt.setInt(4, category.getPostCount());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                category.setId(rs.getInt(1));
            }
        }
    }

    public com.stockportfolio.model.ForumCategory getForumCategoryById(int id) throws SQLException {
        String sql = "SELECT * FROM forum_categories WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                com.stockportfolio.model.ForumCategory category = new com.stockportfolio.model.ForumCategory();
                category.setId(rs.getInt("id"));
                category.setName(rs.getString("name"));
                category.setDescription(rs.getString("description"));
                category.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                category.setPostCount(rs.getInt("post_count"));
                return category;
            }
        }
        return null;
    }

    public List<com.stockportfolio.model.ForumCategory> getAllForumCategories() throws SQLException {
        List<com.stockportfolio.model.ForumCategory> categories = new ArrayList<>();
        String sql = "SELECT * FROM forum_categories ORDER BY name ASC";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                com.stockportfolio.model.ForumCategory category = new com.stockportfolio.model.ForumCategory();
                category.setId(rs.getInt("id"));
                category.setName(rs.getString("name"));
                category.setDescription(rs.getString("description"));
                category.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                category.setPostCount(rs.getInt("post_count"));
                categories.add(category);
            }
        }
        return categories;
    }

    public void updateForumCategory(com.stockportfolio.model.ForumCategory category) throws SQLException {
        String sql = "UPDATE forum_categories SET name = ?, description = ?, post_count = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category.getName());
            pstmt.setString(2, category.getDescription());
            pstmt.setInt(3, category.getPostCount());
            pstmt.setInt(4, category.getId());
            pstmt.executeUpdate();
        }
    }

    public void deleteForumCategory(int id) throws SQLException {
        String sql = "DELETE FROM forum_categories WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    // Forum Post CRUD methods
    public void saveForumPost(com.stockportfolio.model.ForumPost post) throws SQLException {
        String sql = "INSERT INTO forum_posts (user_id, username, category_id, title, content, created_at, updated_at, view_count, comment_count, is_pinned, is_locked) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, post.getUserId());
            pstmt.setString(2, post.getUsername());
            pstmt.setInt(3, post.getCategoryId());
            pstmt.setString(4, post.getTitle());
            pstmt.setString(5, post.getContent());
            pstmt.setTimestamp(6, Timestamp.valueOf(post.getCreatedAt()));
            pstmt.setTimestamp(7, Timestamp.valueOf(post.getUpdatedAt()));
            pstmt.setInt(8, post.getViewCount());
            pstmt.setInt(9, post.getCommentCount());
            pstmt.setBoolean(10, post.isPinned());
            pstmt.setBoolean(11, post.isLocked());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                post.setId(rs.getInt(1));
            }
        }
    }

    public com.stockportfolio.model.ForumPost getForumPostById(int id) throws SQLException {
        String sql = "SELECT * FROM forum_posts WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                com.stockportfolio.model.ForumPost post = new com.stockportfolio.model.ForumPost();
                post.setId(rs.getInt("id"));
                post.setUserId(rs.getInt("user_id"));
                post.setUsername(rs.getString("username"));
                post.setCategoryId(rs.getInt("category_id"));
                post.setTitle(rs.getString("title"));
                post.setContent(rs.getString("content"));
                post.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                post.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                post.setViewCount(rs.getInt("view_count"));
                post.setCommentCount(rs.getInt("comment_count"));
                post.setPinned(rs.getBoolean("is_pinned"));
                post.setLocked(rs.getBoolean("is_locked"));
                return post;
            }
        }
        return null;
    }

    public List<com.stockportfolio.model.ForumPost> getForumPostsByCategoryId(int categoryId) throws SQLException {
        List<com.stockportfolio.model.ForumPost> posts = new ArrayList<>();
        String sql = "SELECT * FROM forum_posts WHERE category_id = ? ORDER BY is_pinned DESC, created_at DESC";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, categoryId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                com.stockportfolio.model.ForumPost post = new com.stockportfolio.model.ForumPost();
                post.setId(rs.getInt("id"));
                post.setUserId(rs.getInt("user_id"));
                post.setUsername(rs.getString("username"));
                post.setCategoryId(rs.getInt("category_id"));
                post.setTitle(rs.getString("title"));
                post.setContent(rs.getString("content"));
                post.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                post.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                post.setViewCount(rs.getInt("view_count"));
                post.setCommentCount(rs.getInt("comment_count"));
                post.setPinned(rs.getBoolean("is_pinned"));
                post.setLocked(rs.getBoolean("is_locked"));
                posts.add(post);
            }
        }
        return posts;
    }

    public List<com.stockportfolio.model.ForumPost> getForumPostsByUserId(int userId) throws SQLException {
        List<com.stockportfolio.model.ForumPost> posts = new ArrayList<>();
        String sql = "SELECT * FROM forum_posts WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                com.stockportfolio.model.ForumPost post = new com.stockportfolio.model.ForumPost();
                post.setId(rs.getInt("id"));
                post.setUserId(rs.getInt("user_id"));
                post.setUsername(rs.getString("username"));
                post.setCategoryId(rs.getInt("category_id"));
                post.setTitle(rs.getString("title"));
                post.setContent(rs.getString("content"));
                post.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                post.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                post.setViewCount(rs.getInt("view_count"));
                post.setCommentCount(rs.getInt("comment_count"));
                post.setPinned(rs.getBoolean("is_pinned"));
                post.setLocked(rs.getBoolean("is_locked"));
                posts.add(post);
            }
        }
        return posts;
    }

    public void updateForumPost(com.stockportfolio.model.ForumPost post) throws SQLException {
        String sql = "UPDATE forum_posts SET title = ?, content = ?, updated_at = ?, comment_count = ?, is_pinned = ?, is_locked = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, post.getTitle());
            pstmt.setString(2, post.getContent());
            pstmt.setTimestamp(3, Timestamp.valueOf(post.getUpdatedAt()));
            pstmt.setInt(4, post.getCommentCount());
            pstmt.setBoolean(5, post.isPinned());
            pstmt.setBoolean(6, post.isLocked());
            pstmt.setInt(7, post.getId());
            pstmt.executeUpdate();
        }
    }

    public void incrementForumPostViewCount(int postId) throws SQLException {
        String sql = "UPDATE forum_posts SET view_count = view_count + 1 WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, postId);
            pstmt.executeUpdate();
        }
    }

    public void incrementForumPostCommentCount(int postId) throws SQLException {
        String sql = "UPDATE forum_posts SET comment_count = comment_count + 1 WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, postId);
            pstmt.executeUpdate();
        }
    }

    public void deleteForumPost(int id) throws SQLException {
        String sql = "DELETE FROM forum_posts WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    // Forum Comment CRUD methods
    public void saveForumComment(com.stockportfolio.model.ForumComment comment) throws SQLException {
        String sql = "INSERT INTO forum_comments (post_id, user_id, username, content, created_at, updated_at, parent_comment_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, comment.getPostId());
            pstmt.setInt(2, comment.getUserId());
            pstmt.setString(3, comment.getUsername());
            pstmt.setString(4, comment.getContent());
            pstmt.setTimestamp(5, Timestamp.valueOf(comment.getCreatedAt()));
            pstmt.setTimestamp(6, Timestamp.valueOf(comment.getUpdatedAt()));
            if (comment.getParentCommentId() != null) {
                pstmt.setInt(7, comment.getParentCommentId());
            } else {
                pstmt.setNull(7, java.sql.Types.INTEGER);
            }
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                comment.setId(rs.getInt(1));
            }
        }
    }

    public com.stockportfolio.model.ForumComment getForumCommentById(int id) throws SQLException {
        String sql = "SELECT * FROM forum_comments WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                com.stockportfolio.model.ForumComment comment = new com.stockportfolio.model.ForumComment();
                comment.setId(rs.getInt("id"));
                comment.setPostId(rs.getInt("post_id"));
                comment.setUserId(rs.getInt("user_id"));
                comment.setUsername(rs.getString("username"));
                comment.setContent(rs.getString("content"));
                comment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                comment.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                int parentId = rs.getInt("parent_comment_id");
                if (!rs.wasNull()) {
                    comment.setParentCommentId(parentId);
                }
                return comment;
            }
        }
        return null;
    }

    public List<com.stockportfolio.model.ForumComment> getForumCommentsByPostId(int postId) throws SQLException {
        List<com.stockportfolio.model.ForumComment> comments = new ArrayList<>();
        String sql = "SELECT * FROM forum_comments WHERE post_id = ? ORDER BY created_at ASC";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, postId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                com.stockportfolio.model.ForumComment comment = new com.stockportfolio.model.ForumComment();
                comment.setId(rs.getInt("id"));
                comment.setPostId(rs.getInt("post_id"));
                comment.setUserId(rs.getInt("user_id"));
                comment.setUsername(rs.getString("username"));
                comment.setContent(rs.getString("content"));
                comment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                comment.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                int parentId = rs.getInt("parent_comment_id");
                if (!rs.wasNull()) {
                    comment.setParentCommentId(parentId);
                }
                comments.add(comment);
            }
        }
        return comments;
    }

    public void updateForumComment(com.stockportfolio.model.ForumComment comment) throws SQLException {
        String sql = "UPDATE forum_comments SET content = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, comment.getContent());
            pstmt.setTimestamp(2, Timestamp.valueOf(comment.getUpdatedAt()));
            pstmt.setInt(3, comment.getId());
            pstmt.executeUpdate();
        }
    }

    public void deleteForumComment(int id) throws SQLException {
        String sql = "DELETE FROM forum_comments WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    private void populateFortune500Stocks() throws SQLException {
        // Top 50 Fortune 500/blue-chip stocks data
        String[][] fortune500Stocks = {
            {"AAPL", "Apple Inc.", "NASDAQ", "Technology"},
            {"MSFT", "Microsoft Corporation", "NASDAQ", "Technology"},
            {"GOOGL", "Alphabet Inc.", "NASDAQ", "Technology"},
            {"AMZN", "Amazon.com Inc.", "NASDAQ", "Consumer Discretionary"},
            {"TSLA", "Tesla Inc.", "NASDAQ", "Consumer Discretionary"},
            {"NVDA", "NVIDIA Corporation", "NASDAQ", "Technology"},
            {"META", "Meta Platforms Inc.", "NASDAQ", "Technology"},
            {"NFLX", "Netflix Inc.", "NASDAQ", "Communication Services"},
            {"JPM", "JPMorgan Chase & Co.", "NYSE", "Financials"},
            {"BAC", "Bank of America Corporation", "NYSE", "Financials"},
            {"WMT", "Walmart Inc.", "NYSE", "Consumer Staples"},
            {"KO", "The Coca-Cola Company", "NYSE", "Consumer Staples"},
            {"PFE", "Pfizer Inc.", "NYSE", "Health Care"},
            {"JNJ", "Johnson & Johnson", "NYSE", "Health Care"},
            {"V", "Visa Inc.", "NYSE", "Financials"},
            {"MA", "Mastercard Incorporated", "NYSE", "Financials"},
            {"HD", "The Home Depot Inc.", "NYSE", "Consumer Discretionary"},
            {"DIS", "The Walt Disney Company", "NYSE", "Communication Services"},
            {"BA", "The Boeing Company", "NYSE", "Industrials"},
            {"CAT", "Caterpillar Inc.", "NYSE", "Industrials"},
            {"XOM", "Exxon Mobil Corporation", "NYSE", "Energy"},
            {"CVX", "Chevron Corporation", "NYSE", "Energy"},
            {"PG", "The Procter & Gamble Company", "NYSE", "Consumer Staples"},
            {"MCD", "McDonald's Corporation", "NYSE", "Consumer Discretionary"},
            {"NKE", "Nike Inc.", "NYSE", "Consumer Discretionary"},
            {"ORCL", "Oracle Corporation", "NYSE", "Technology"},
            {"CRM", "Salesforce Inc.", "NYSE", "Technology"},
            {"ADBE", "Adobe Inc.", "NASDAQ", "Technology"},
            {"INTC", "Intel Corporation", "NASDAQ", "Technology"},
            {"CSCO", "Cisco Systems Inc.", "NASDAQ", "Technology"},
            {"IBM", "International Business Machines Corporation", "NYSE", "Technology"},
            {"TXN", "Texas Instruments Incorporated", "NASDAQ", "Technology"},
            {"QCOM", "QUALCOMM Incorporated", "NASDAQ", "Technology"},
            {"AMD", "Advanced Micro Devices Inc.", "NASDAQ", "Technology"},
            {"LLY", "Eli Lilly and Company", "NYSE", "Health Care"},
            {"ABBV", "AbbVie Inc.", "NYSE", "Health Care"},
            {"MRK", "Merck & Co. Inc.", "NYSE", "Health Care"},
            {"TMO", "Thermo Fisher Scientific Inc.", "NYSE", "Health Care"},
            {"DHR", "Danaher Corporation", "NYSE", "Health Care"},
            {"UNH", "UnitedHealth Group Incorporated", "NYSE", "Health Care"},
            {"CVS", "CVS Health Corporation", "NYSE", "Health Care"},
            {"CI", "The Cigna Group", "NYSE", "Health Care"},
            {"HUM", "Humana Inc.", "NYSE", "Health Care"},
            {"ANTM", "Anthem Inc.", "NYSE", "Health Care"},
            {"BMY", "Bristol-Myers Squibb Company", "NYSE", "Health Care"},
            {"GILD", "Gilead Sciences Inc.", "NASDAQ", "Health Care"},
            {"REGN", "Regeneron Pharmaceuticals Inc.", "NASDAQ", "Health Care"},
            {"VRTX", "Vertex Pharmaceuticals Incorporated", "NASDAQ", "Health Care"},
            {"BIIB", "Biogen Inc.", "NASDAQ", "Health Care"},
            {"AMGN", "Amgen Inc.", "NASDAQ", "Health Care"}
        };

        try (Connection conn = dataSource.getConnection()) {
            String sql = "MERGE INTO stocks (symbol, name, exchange, sector, current_price, previous_close, change_value, change_percent, volume, market_cap, last_updated) KEY (symbol) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (String[] stock : fortune500Stocks) {
                    // Check if stock already exists
                    if (getStockBySymbol(stock[0]) != null) {
                        continue; // Skip if already exists
                    }

                    pstmt.setString(1, stock[0]); // symbol
                    pstmt.setString(2, stock[1]); // name
                    pstmt.setString(3, stock[2]); // exchange
                    pstmt.setString(4, stock[3]); // sector

                    // Set default values for price data (will be updated by real-time API)
                    double basePrice = 100.0 + Math.random() * 900.0; // Random price between 100-1000
                    double change = (Math.random() - 0.5) * 20.0; // Random change between -10 and +10
                    double changePercent = (change / basePrice) * 100.0;

                    pstmt.setDouble(5, basePrice); // current_price
                    pstmt.setDouble(6, basePrice - change); // previous_close
                    pstmt.setDouble(7, change); // change_value
                    pstmt.setDouble(8, changePercent); // change_percent
                    pstmt.setLong(9, (long) (Math.random() * 10000000) + 1000000); // volume
                    pstmt.setLong(10, (long) (basePrice * 1000000)); // market_cap (simplified)
                    pstmt.setTimestamp(11, Timestamp.valueOf(java.time.LocalDateTime.now()));

                    try {
                        pstmt.executeUpdate();
                    } catch (SQLException e) {
                        // Handle potential duplicate key errors gracefully
                        System.err.println("Error inserting stock " + stock[0] + ": " + e.getMessage());
                    }
                }
            }
        }
        System.out.println("Fortune 500 stocks populated successfully.");
    }
}
