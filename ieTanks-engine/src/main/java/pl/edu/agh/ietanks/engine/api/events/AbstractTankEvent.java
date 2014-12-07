package pl.edu.agh.ietanks.engine.api.events;


import pl.edu.agh.ietanks.engine.api.Direction;

public abstract class AbstractTankEvent implements Event {

    protected final String tankId;
    protected final Direction direction;
    private final TankAction action;
    public AbstractTankEvent(TankAction action, String tankId, Direction direction) {
        this.action = action;
        this.tankId = tankId;
        this.direction = direction;
    }

    public Direction getDirection() {
        return direction;
    }

    public TankAction getAction() {
        return action;
    }

    @Override
    public EventGroup getEventGroup() {
        return EventGroup.TANKS;
    }

    public String tankId() {
        return tankId;
    }

    public enum TankAction{
        DESTROYED("destroyed"),
        MOVED("moved"),
        BUMPED_INTO_WALL("bumped"),
        NOT_MOVED("none"),
        ;

        private String actionName;
        private TankAction(String actionName){
            this.actionName = actionName;
        }

        public String getActionName() {
            return actionName;
        }
    }
}
