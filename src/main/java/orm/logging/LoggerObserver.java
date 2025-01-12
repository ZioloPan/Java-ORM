package orm.logging;

public class LoggerObserver implements Observer {

    @Override
    public void notify(String message) {
        System.out.println("[LOG] " + message);
    }
}
