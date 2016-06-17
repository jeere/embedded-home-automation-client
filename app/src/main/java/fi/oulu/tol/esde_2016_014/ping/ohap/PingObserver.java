package fi.oulu.tol.esde_2016_014.ping.ohap;

public interface PingObserver {
    public void handlePingResponse(IncomingMessage response);
}
