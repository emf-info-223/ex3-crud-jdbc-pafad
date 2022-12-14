package app.workers;

import app.beans.Personne;
import app.exceptions.MyDBException;
import app.helpers.SystemLib;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DbWorker implements DbWorkerItf {

    private Connection dbConnexion;
    private List<Personne> listePersonnes;
    
    /**
     * Constructeur du worker
     */
    public DbWorker() {
        listePersonnes = new ArrayList<>();
    }

    @Override
    public void connecterBdMySQL(String nomDB) throws MyDBException {
        final String url_local = "jdbc:mysql://localhost:3306/" + nomDB;
        final String url_remote = "jdbc:mysql://LAPEMFB37-21.edu.net.fr.ch:3306/" + nomDB;
        final String user = "root";
        final String password = "emf123";

        System.out.println("url:" + url_local);
        try {
            dbConnexion = DriverManager.getConnection(url_local, user, password);
        } catch (SQLException ex) {
            throw new MyDBException(SystemLib.getFullMethodName(), ex.getMessage());
        }
    }

    @Override
    public void connecterBdHSQLDB(String nomDB) throws MyDBException {
        final String url = "jdbc:hsqldb:file:" + nomDB + ";shutdown=true";
        final String user = "SA";
        final String password = "";
        System.out.println("url:" + url);
        try {
            dbConnexion = DriverManager.getConnection(url, user, password);
        } catch (SQLException ex) {
            throw new MyDBException(SystemLib.getFullMethodName(), ex.getMessage());
        }
    }

    @Override
    public void connecterBdAccess(String nomDB) throws MyDBException {
        final String url = "jdbc:ucanaccess://" + nomDB;
        System.out.println("url=" + url);
        try {
            dbConnexion = DriverManager.getConnection(url);
        } catch (SQLException ex) {
            throw new MyDBException(SystemLib.getFullMethodName(), ex.getMessage());
        }
    }

    @Override
    public void deconnecter() throws MyDBException {
        try {
            if (dbConnexion != null) {
                dbConnexion.close();
            }
        } catch (SQLException ex) {
            throw new MyDBException(SystemLib.getFullMethodName(), ex.getMessage());
        }
    }

    @Override
    public List<Personne> lirePersonnes() throws MyDBException { 
        //Requ??te vide
        PreparedStatement requete;
        
        try{
            //Pour ??viter des injections SQL
            requete = dbConnexion.prepareStatement(
                    "select PK_PERS, Nom, Prenom, Date_Naissance, No_rue, Rue, NPA, Ville, Actif, Salaire, date_modif from t_personne"
            );
        
            //Execution de la requ??te
            ResultSet rs = requete.executeQuery();
            
            //Pour chaque r??sultats
            while(rs.next()){
                listePersonnes.add(new Personne(
                        rs.getInt("PK_PERS"), //int pkPers 
                        rs.getString("Nom"), //String nom
                        rs.getString("Prenom"), //String prenom
                        rs.getDate("Date_Naissance"), //Date dateNaissance
                        rs.getInt("No_rue"), //int noRue
                        rs.getString("Rue"), //String rue
                        rs.getInt("NPA"), //int npa
                        rs.getString("Ville"), //String localite
                        rs.getBoolean("Actif"), //boolean actif
                        rs.getDouble("Salaire"), //double salaire
                        rs.getDate("date_modif") //Date dateModif
                )); //Ajout de la personne
            }
            
        } catch (SQLException ex){
            throw new MyDBException(SystemLib.getFullMethodName(), ex.getMessage());
        }
        
        //Retour de la liste de personnes
        return listePersonnes;
    }
    
    @Override
    public Personne lire(int index) throws MyDBException {
        //Lire la personne ?? l'index actuelle
        try{
            //Pour ??viter des injections SQL
            PreparedStatement requete = dbConnexion.prepareStatement(
                    "select PK_PERS, Nom, Prenom, Date_Naissance, No_rue, Rue, NPA, Ville, Actif, Salaire, date_modif from t_personne WHERE PK_PERS = ?"
            );
            
            //On place la pk que l'on va rechercher
            requete.setInt(1, index);
            
            //Execution de la requ??te
            ResultSet resultat = requete.executeQuery();
            
            //S'il y a des r??sultats
            if(resultat.next()){
                
                //Je cr??e un boolean pour savoir si j'ai une correspondence
                boolean match = false;
                
                //On garde au chaud le r??sultat de cette m??thode
                Personne resultatPersonne = null;
                
                //R??cup??ration des info de la personne dans la base de donn??es
                int pk = resultat.getInt("PK_PERS");
                
                //Recherche dans la liste de personne
                for(Personne personne : listePersonnes){
                    
                    //Je compare les pk
                    if(personne.getPkPers() == pk){
                        
                        //Je mets mon r??sultat ici, c'est ?? dire dans la d??claration faite plus haute
                        resultatPersonne = personne;
                        
                        //Je passe mon match ?? true
                        match = true;
                        
                        //Je casse la boucle une fois trouv??
                        break;
                    } 
                       
                }
                
                //Si j'ai une correspondance
                if(match){
                    
                    //Je retourne la personne ayant cette pk
                    return resultatPersonne;
                    
                } else {
                    
                    //Si j'ai vraiment aucun match je retourne null
                    return null;
                    
                }
                
            } else {
                
                //Si aucun r??sultat
                return null;
                
            }
            
        } catch (SQLException ex){
            throw new MyDBException(SystemLib.getFullMethodName(), ex.getMessage());
        } 
    }
    
    @Override
    public void modifier(Personne personne) throws MyDBException {
        
        //Requ??te vide
        PreparedStatement requete;
        
        //Construction de la requ??te finale en fonction de ce qu'il y a comme modification, cette requ??te peut ??conomiser des perfomances en comparant les donn??es
        String requeteFinale = "UPDATE t_personne SET ";
        
        try{
            
            //Pr??paration de la requ??te pour ??viter les injections
            requete = dbConnexion.prepareStatement("select PK_PERS, Nom, Prenom, Date_Naissance, No_rue, Rue, NPA, Ville, Actif, Salaire, date_modif, no_modif from t_personne WHERE PK_PERS = " + personne.getPkPers());
            
            //Execution du select pour comparer les donn??es
            ResultSet rs = requete.executeQuery();
            
            //Si les modifs sont pr??sentes
            boolean modif = false;
            
            //S'il y a des r??sultats
            while(rs.next()){
                
                //Si le nom a ??t?? modifi??
                if(!personne.getNom().equals(rs.getString("Nom"))){
                    
                    //Ajout de la requ??te modifiant le nom
                    requeteFinale += "Nom = '" + personne.getNom() + "',\n";
                    
                    //Il y a une modification
                    modif = true;
                }
                
                //Si le pr??nom a ??t?? modifi??
                if(!personne.getPrenom().equals(rs.getString("Prenom"))){
                    
                    //Ajout de la requ??te modifiant le pr??nom
                    requeteFinale += "Prenom = '" + personne.getPrenom() + "',\n";
                    
                    //Il y a une modification
                    modif = true;
                }
                
                //Si la date de naissance a ??t?? modifi??
                if(!personne.getDateNaissance().equals(rs.getDate("Date_naissance"))){
                    
                    //Ajout de la requ??te modifiant la date de naissance
                    requeteFinale += "Date_naissance = '" + personne.getDateNaissance() + "',\n";
                
                    //Il y a une modification
                    modif = true;
                }
                
                //Si le num??ro de rue a ??t?? modifi??
                if(personne.getNoRue() != rs.getInt("No_rue")){
                    
                    //Ajout de la requ??te modifiant le num??ro de rue
                    requeteFinale += "No_rue = " + personne.getNoRue() + ",\n";
                
                    //Il y a une modification
                    modif = true;
                }
                
                //Si la rue a ??t?? modifi??
                if(!personne.getRue().equals(rs.getString("Rue"))){
                    
                    //Ajout de la requ??te modifiant la rue
                    requeteFinale += "Rue = '" + personne.getRue() + "',\n";
                    
                    //Il y a une modification
                    modif = true;
                }
                
                //Si le code postal a ??t?? modifi??
                if(personne.getNpa() != rs.getInt("NPA")){
                    
                    //Ajout de la requ??te modifiant le code postal
                    requeteFinale += "NPA = " + personne.getNpa() + ",\n";
                    
                    //Il y a une modification
                    modif = true;
                }
                
                //Si la ville a ??t?? modifi??
                if(!personne.getLocalite().equals(rs.getString("Ville"))){
                    
                    //Ajout de la requ??te modifiant la ville
                    requeteFinale += "Ville = '" + personne.getLocalite() + "',\n";
                    
                    //Il y a une modification
                    modif = true;
                }
                
                //Si le boolean actif a ??t?? modifi??
                if(personne.isActif() != rs.getBoolean("Actif")){
                    
                    //Ajout de la requ??te modifiant l'activit??
                    requeteFinale += "Actif = " + personne.isActif() + ",\n";
                    
                    //Il y a une modification
                    modif = true;
                }
                
                //Si le salaire a ??t?? modifi??
                if(personne.getSalaire() != rs.getDouble("Salaire")){
                    
                    //Ajout de la requ??te modifiant le salaire
                    requeteFinale += "Salaire = " + personne.getSalaire() + ",\n";
                    
                    //Il y a une modification
                    modif = true;
                }
                
                //S'il y a eu au moins une modification
                if(modif){
                    
                    //Mise ?? jour de la date de modification
                    requeteFinale += "date_modif = '" + Date.valueOf(LocalDate.now()) + "',\n";
                    
                    //Mise ?? jour du num??ro de la modification
                    requeteFinale += "no_modif = " + (rs.getInt("no_modif") + 1) + "\n";
                    
                    //Condition de la requ??te
                    requeteFinale += " WHERE PK_PERS = " + personne.getPkPers() + ";";
                
                    //Pour tester elle affiche la requ??te finale construite
                    System.out.println(requeteFinale);
                
                    //Preparation de la requ??te pour ??viter les injections comme toujours
                    requete = dbConnexion.prepareStatement(requeteFinale);
                
                    //On met tout ?? jour avec cette m??thode
                    requete.executeUpdate();
                
                } else { //S'il n'y a pas de modification
                    
                    //On vire la requ??te
                    requeteFinale = "";
                }
                         
            }
            
        } catch(SQLException ex){
            throw new MyDBException(SystemLib.getFullMethodName(), ex.getMessage());
        }
    }
    
    @Override
    public void effacer(Personne personne) throws MyDBException {
        
        //Une requ??te vide
        PreparedStatement st;
        
        try{
            //Pr??paration de la requ??te
            st = dbConnexion.prepareStatement("delete from t_personne where PK_PERS = ?");
            
            //On y place la pk que l'on veut supprimer ?? la fin
            st.setInt(1, personne.getPkPers());
            
            //On execute la requ??te
            st.executeUpdate();
            
        }catch (SQLException ex){
            throw new MyDBException(SystemLib.getFullMethodName(), ex.getMessage());
        }
    }
    
    @Override
    public void creer(Personne personne) throws MyDBException {
        
        //Une requ??te vide
        PreparedStatement st;
        
        try{
            
            //Pour ??viter les injections SQL
            st = dbConnexion.prepareStatement(
                    "insert into t_personne("
                            + "nom, prenom, date_naissance, no_rue, rue, npa, ville, actif, salaire, date_modif, no_modif"
                            + ") VALUES (?,?,?,?,?,?,?,?,?,?,?)"
            );
            
            //On place toute les valeurs justes dans la requ??te en fonction de la position
            st.setString(1, personne.getNom()); //Nom
            st.setString(2, personne.getPrenom()); //Pr??nom
            st.setDate(3, convertirDate(personne.getDateNaissance()));//Date de naissance
            st.setInt(4, personne.getNoRue()); //Num??ro de rue
            st.setString(5, personne.getRue()); //La rue
            st.setInt(6, personne.getNpa()); //Le code postal
            st.setString(7, personne.getLocalite()); //La ville
            st.setBoolean(8, personne.isActif()); //La personne est active ou non 
            st.setDouble(9, personne.getSalaire()); //Le salaire
            st.setDate(10, convertirDate(personne.getDateModif())); //La date de la modif
            st.setInt(11, 0); //Le num??ro de la modif
        
            //J'execute la requ??te avec executeUpdate, elle est pour des mise ?? jours dont les insertion, update et delete
            st.executeUpdate();
            
        } catch (SQLException ex){
            throw new MyDBException(SystemLib.getFullMethodName(), ex.getMessage());
        }
    }

    /**
     * Permet la convetion de java.util.Date en java.sql.Date
     * @param date la date (java.util.Date)
     * @return La date (java.sql.Date)
     */
    public java.sql.Date convertirDate(java.util.Date date){
        
        //Je transforme la date en long puis je la place dans une instance de java.sql.Date
        return new java.sql.Date(date.getTime());   
        
    }
    
}
