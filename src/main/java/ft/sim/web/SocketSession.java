package ft.sim.web;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ft.sim.simulation.BasicSimulation;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Created by Sina on 27/02/2017.
 */
public class SocketSession {

  private final WebSocketSession session;

  private static final Gson gson = new Gson();

  private Logger logger = LoggerFactory.getLogger(SocketSession.class);

  public SocketSession(WebSocketSession session) {
    this.session = session;
  }

  public String getResponse(String message) {

    /*if (message.equals("start trains")) {
      sim.startTrains();
      return "OK";
    }*/

    BasicSimulation simulation = BasicSimulation.getInstance();
    /*if (simulation.isKilled()) {
      simulation = BasicSimulation.newInstance();
    }*/
    logger.info("message: {}", message);

    try {
      Type stringStringMap = new TypeToken<Map<String, String>>() {
      }.getType();
      Map<String, String> map = gson.fromJson(message, stringStringMap);

      if (map.containsKey("command")) {
        return processCommand(simulation, map) ? "OK" : "FAIL";
      }
      if (map.containsKey("type") && map.get("type").equals("set")) {
        return processSetCommand(simulation, map) ? "OK" : "FAIL";
      }
    } catch (com.google.gson.JsonSyntaxException ex) {
      // wasn't json
      ex.printStackTrace();
    }

    return "echo: " + message;
  }

  private boolean processSetCommand(BasicSimulation simulation, Map<String, String> map) {
    String set = map.get("set");
    switch (set) {
      case "trainTargetSpeed":
        int trainID = Integer.valueOf(map.get("targetID"));
        double targetSpeed = Double.valueOf(map.get("data"));
        simulation.getWorld().getTrain(trainID).getEngine()
            .setTargetSpeed(targetSpeed);
        return true;
      case "worldMap":
        String mapKey = map.get("data");
        if (simulation == null) {
          BasicSimulation.getInstance(mapKey);
        } else {
          simulation.setWorld(mapKey);
        }
        return true;
    }
    return false;
  }

  private boolean processCommand(BasicSimulation simulation, Map<String, String> map) {
    String command = map.get("command");
    switch (command) {
      case "start trains":
        simulation.startTrains();
        return true;
      case "stop simulation":
        simulation.removeSocketSessions(this);
        simulation.kill();
        return true;
      case "get push data":
        if (simulation != null) {
          simulation.setSocketSession(this);
          return true;
        }
        return false;
      case "start simulation":
        simulation.startSimulation();
        simulation.setSocketSession(this);
        return true;
      case "toggle interactive":
        simulation.toggleInteractive();
        return true;

    }
    return false;
  }

  public WebSocketSession getSession() {
    return session;
  }

  void boop() {
    int i = 1;
    while (i < 10) {
      TextMessage m = new TextMessage("boop: " + i);
      try {
        session.sendMessage(m);

      } catch (IOException e) {
        e.printStackTrace();
      }
      i++;
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

}
