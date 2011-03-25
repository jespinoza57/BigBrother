package me.taylorkelly.bigbrother.tablemgrs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import me.taylorkelly.bigbrother.BBLogging;
import me.taylorkelly.bigbrother.BBPlayerInfo;
import me.taylorkelly.bigbrother.datasource.ConnectionManager;

public class BBUsersSQLite extends BBUsersTable {
    
    @Override
    protected BBPlayerInfo getUserFromDB(String name) {

        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            conn = ConnectionManager.getConnection();
            ps = conn.prepareStatement("SELECT id,name,flags FROM "+getTableName()+" WHERE LOWER(`name`)=LOWER(?);");
            ps.setString(0,name);
            rs=ps.executeQuery();
            
            if(!rs.next())
                return null;
            
            return new BBPlayerInfo(rs.getInt(0),rs.getString(1),rs.getInt(2));
            
        } catch (SQLException e) {
            BBLogging.severe("Can't find the user `"+name+"`.", e);
        } finally {
            ConnectionManager.cleanup( "BBUsersSQLite.getUserFromDB(string)",conn, ps, rs );
        }
        return null;
    }
    
    @Override
    protected void onLoad() {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public String getCreateSyntax() {
        return "CREATE TABLE `"+getTableName()+"` ("
        + "`id` INT NOT NULL AUTO_INCREMENT," 
        + "`name` varchar(32) NOT NULL DEFAULT 'Player'," 
        + "`flags` INT NOT NULL DEFAULT '0',"
        + "PRIMARY KEY (`id`));" //Engine doesn't matter, really.
        + "CREATE UNIQUE INDEX idxUsername ON `name`;";
    }

    @Override
    protected void do_addOrUpdatePlayer(BBPlayerInfo pi) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = ConnectionManager.getConnection();
            if(pi.getNew()) {
                ps = conn.prepareStatement("INSERT INTO "+getTableName()+" (name,flags) VALUES (?,?)");
                ps.setString(0,pi.getName());
                ps.setInt(1,pi.getFlags());
            } else {
                ps = conn.prepareStatement("UPDATE "+getTableName()+" SET flags = ? WHERE id=?");
                ps.setInt(0, pi.getFlags());
                ps.setInt(1, pi.getID());
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            BBLogging.severe("Can't update the user `"+pi.getName()+"`.", e);
        } finally {
            ConnectionManager.cleanup( "BBUsersSQLite.do_addOrUpdatePlayer",conn, ps, null );
        }
    }

    @Override
    protected BBPlayerInfo getUserFromDB(int id) {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            conn = ConnectionManager.getConnection();
            ps = conn.prepareStatement("SELECT id,name,flags FROM "+getTableName()+" WHERE `id`=?;");
            ps.setInt(0,id);
            rs=ps.executeQuery();
            
            if(!rs.next())
                return null;
            
            return new BBPlayerInfo(rs.getInt(0),rs.getString(1),rs.getInt(2));
            
        } catch (SQLException e) {
            BBLogging.severe("Can't find user #"+id+".", e);
        } finally {
            ConnectionManager.cleanup( "BBUsersSQLite.getUserFromDB(int)",conn, ps, rs );
        }
        return null;
    }

    @Override
    public void importRecords() {
        String bbdata = BBDataTable.getInstance().getTableName();
        BBLogging.info("Importing users into new table!");

        BBLogging.info(" * Stage 1/4: Rename the old player column to PlayerName");
        if(!executeUpdate("importRecords(sqlite) - Rename old player column",
                "ALTER TABLE "+bbdata+" CHANGE player playerName varchar(32) NOT NULL DEFAULT 'Player'"))
            return;

        BBLogging.info(" * Stage 2/4: Add player column with the new integer format.");
        if(!executeUpdate("importRecords(sqlite) - Add player column",
                "ALTER TABLE "+bbdata+" ADD COLUMN player NOT NULL INT UNSIGNED DEFAULT 0;"))
            return;
        

        BBLogging.info(" * Stage 3/4: Convert player name -> player ID in bbdata while adding users to bbusers.");
        {
            Connection conn = null;
            ResultSet rs = null;
            PreparedStatement ps = null;
            try {
                conn = ConnectionManager.getConnection();
                ps = conn.prepareStatement("SELECT DISTINCT playerName FROM "+bbdata);
                rs=ps.executeQuery();
                
                while(rs.next()) {
                    BBPlayerInfo pi = getUser(rs.getString(0));
                    executeUpdate(
                            String.format("Player %s -> %d",pi.getName(),pi.getID()),
                            "UPDATE "+bbdata+" SET `player`=? WHERE LOWER(playerName)=LOWER('?')", new Object[]{
                                pi.getID(),
                                pi.getName()
                            }
                    );
                }
                
            } catch (SQLException e) {
                BBLogging.severe("Can't import old user records.", e);
                return;
            } finally {
                ConnectionManager.cleanup( "BBUsersSQLite.getUserFromDB(int)",conn, ps, rs );
            }
        }
        

        BBLogging.info(" * Stage 4/4: Drop playerName column.");
        if(!executeUpdate("importRecords(sqlite) - Add player column",
                "ALTER TABLE "+bbdata+" DROP COLUMN playerName;"))
            return;
        
    }
    
}