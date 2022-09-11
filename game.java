import org.neo4j.driver.*;

import java.util.Random;
import java.util.Scanner;

import static org.neo4j.driver.Values.parameters;

class HelloWorldExample implements AutoCloseable
{
    private final Driver driver;

    public HelloWorldExample( String uri, String user, String password )
    {
        driver = GraphDatabase.driver( uri, AuthTokens.basic( user, password ) );
    }

    @Override
    public void close() throws Exception
    {
        driver.close();
    }

    public void createNodes()
    {
        try
        {
            Session session=driver.session(SessionConfig.forDatabase("test"));
            session.writeTransaction( tx ->
            {
                Result result = tx.run( "MERGE  (scissors:object {name:'scissors',id:2})-[:cuts]->(paper:object {name:'paper',id:3})-[:covers]->(rock:object {name:'rock',id:1})-[:crushes]->(lizard:animal {name:'lizard',id:4})-[:poisons]->(spock:vulcan:person {name:'spock',id:5})-[:smashes]->(scissors)-[:decapitates]->(lizard)-[:eats]->(paper)-[:disproves]->(spock)-[:vaporizes]->(rock)-[:crushes]->(scissors) return scissors,rock,spock,paper,lizard");
                System.out.println(result);
                return result;
            } );
        }
        finally {

        }
    }
    public String findWinner(int computerChoice,int userChoice){
        Session session=driver.session(SessionConfig.forDatabase("test"));
        int relation=session.readTransaction(tx -> {
            Result result = tx.run("match ({id:$userChoice})-[r]->({id:$computerChoice}) return SIGN(COUNT(r))",parameters("userChoice",userChoice,"computerChoice",computerChoice));
            return  Integer.parseInt(result.single().get(0).toString());
        });
        if(relation>0){
            return ("congrates you win");
        }
        else{
           return ("computer win :P");
        }

    }
    public void playGame(){
        int userChoice;
        do {
                System.out.println("please choose the number of your choice: \n 1.Rock\n 2.Scissors\n 3.Paper\n 4.Lizard \n 5.spock \n 6.Exit");
                Scanner sc = new Scanner(System.in);
                do {
                    System.out.println("Please enter your choice!");
                    while (!sc.hasNextInt()) {
                        System.out.println("That's not a number!");
                        sc.next(); // this is important!
                    }
                    userChoice = sc.nextInt();
                } while (userChoice < 0 || userChoice > 6);
                if (userChoice == 6) System.exit(0);
                Random random = new Random();
                int computerChoice = random.nextInt(5 - 1 + 1) + 1;
                System.out.println("the computer chose " + getName(computerChoice));
                if (computerChoice == userChoice)
                    System.out.println("Draw try again");
                else
                    System.out.println(findWinner(computerChoice, userChoice));
            }while (true);
    }
    public String getName(int number){
            switch (number){
                case 1:return "Rock";
                case 2:return "Scissors";
                case 3:return "Paper";
                case 4:return "Lizard";
                default:return "Spock";
            }
    }
    public static void main( String... args ) throws Exception
    {
        try(HelloWorldExample greeter = new HelloWorldExample( "bolt://localhost:7687", "admin", "admin" ))
        {

             greeter.createNodes();
             greeter.playGame();

        }finally {

        }
    }
}
