package dat.startcode.model.persistence;
/*import dat.startcode.entities.User;
import dat.startcode.exceptions.DatabaseException;*/
import dat.startcode.model.entities.Customer;
import dat.startcode.model.entities.User;
import dat.startcode.model.exceptions.DatabaseException;
import dat.startcode.model.persistence.ConnectionPool;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.rmi.server.LogStream.log;

public class UserMapper
{
    ConnectionPool connectionPool;

    public UserMapper(ConnectionPool connectionPool)
    {
        this.connectionPool = connectionPool;
    }


    public User login(String email, String password) throws DatabaseException {

        //createUser("mogens@lykketoft.dk", "Mogens Lykketoft", "MogensErGud","admin");
        Logger.getLogger("web").log(Level.INFO, "");

        User user = null;
        System.out.println(email);

        String sql = "SELECT * FROM `user` WHERE email = ? ";
        int userId;
        byte[] hash;
        byte[] salt;
        String name;
        int role;
        int phone;
        int balance;

        try (Connection connection = connectionPool.getConnection())
        {
            try (PreparedStatement ps = connection.prepareStatement(sql))
            {
                ps.setString(1, email);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    System.out.println("ello");
                    userId = rs.getInt("user_id");
                    hash = rs.getBytes("Hash");
                    salt = rs.getBytes("Salt");
                    name = rs.getString("name");
                    role = rs.getInt("role");
                    phone = rs.getInt("phone");
                    balance = rs.getInt("balance");

                } else
                {
                    throw new DatabaseException("Fejl i brugernavn eller kodeord");
                }

                KeySpec check = new PBEKeySpec(password.toCharArray(),salt,65536,128);
                SecretKeyFactory factory1 = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                byte[] userPassword = factory1.generateSecret(check).getEncoded();
                if(Arrays.equals(hash,userPassword)){
                    user = new User(userId,name,email,role,phone,balance);
                    System.out.println("Hej det er faktisk rigtigt password");
                }
                else{
                    System.out.println("Wrong Password");
                    return null;
                }
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                e.printStackTrace();
            }
        } catch (SQLException ex)
        {
            throw new DatabaseException(ex, "Fejl under indlogning. Der er noget galt med databasen");
        }
        return user;
    }


    public User createUser(String name, String email, String password, int role,int phone,int balance) throws DatabaseException
    {
        Logger.getLogger("web").log(Level.INFO, "");
        User user;
        String sql = "insert into `user` ( name, email, Hash, Salt, role,phone, balance) values (?,?,?,?,?,?,?)";

        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        byte[] hash = new byte[16];
        random.nextBytes(salt);

        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt , 65536, 128);

        SecretKeyFactory factory;
        try {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            hash = factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }

        try (Connection connection = connectionPool.getConnection())
        {
            try (PreparedStatement ps = connection.prepareStatement(sql))
            {
                ps.setString(1, name);
                ps.setString(2, email);
                ps.setBytes(3,hash);
                ps.setBytes(4,salt);
                ps.setInt(5, role);
                ps.setInt(6,phone);
                ps.setInt(7,balance);
                int rowsAffected = ps.executeUpdate();
                int maxRows = ps.getMaxRows();
                if (rowsAffected == 1)
                {
                    user = new User(maxRows,name,email, role,phone,balance);
                } else
                {
                    throw new DatabaseException("Brugeren med email = " + email + " kunne ikke oprettes i databasen");
                }
            }
        }
        catch (SQLException ex)
        {
            throw new DatabaseException(ex, "Kunne ikke indsætte bruger i databasen");
        }
        return user;
    }
    public static void createUserTest(ConnectionPool connectionPool){
        UserMapper userMapper = new UserMapper(connectionPool);
        try {
            userMapper.createUser("Mogens Lykketoft","mogens@lykketoft.dk", "MogensErGud",0,20202020,1000);
        } catch (DatabaseException e) {
            e.printStackTrace();
        }

    }

    public void updateUser(User user){

        Logger.getLogger("web").log(Level.INFO, "");

        String sql = "update `user` set `name` = ?, email = ?, phone = ?, balance = ? where user_id = ?;";

        try (Connection connection = connectionPool.getConnection())
        {
            try (PreparedStatement ps = connection.prepareStatement(sql))
            {
                ps.setString(1, user.getName());
                ps.setString(2, user.getEmail());
                ps.setInt(3,user.getPhoneNumber());
                ps.setInt(4,user.getBalance());
                ps.setInt(5,user.getUserId());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Customer> getAllUsers(){

        Logger.getLogger("web").log(Level.INFO, "");
        String sql = "SELECT * FROM `user`;";

        ArrayList<Customer> customers = new ArrayList<>();

        try (Connection connection = connectionPool.getConnection())
        {
            try (PreparedStatement ps = connection.prepareStatement(sql))
            {

                ResultSet rs = ps.executeQuery();

                while (rs.next()){
                    int userId = rs.getInt("user_id");
                    String name = rs.getString("name");
                    String email = rs.getString("email");
                    int phoneNumber = rs.getInt("phone");
                    int balance = rs.getInt("balance");
                    customers.add(new Customer(userId,name,email,phoneNumber,balance));
                }


            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return customers;
    }

    public void updateCustomerBalance (int userId, int balance){

        Logger.getLogger("web").log(Level.INFO, "");
        String sql = "UPDATE `user` set balance = balance + ? where user_id = ?;";

        try (Connection connection = connectionPool.getConnection())
        {
            try (PreparedStatement ps = connection.prepareStatement(sql))
            {
                ps.setInt(1,balance);
                ps.setInt(2,userId);

                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }





}
