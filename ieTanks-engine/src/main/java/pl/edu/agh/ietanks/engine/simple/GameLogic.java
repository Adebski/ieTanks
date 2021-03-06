package pl.edu.agh.ietanks.engine.simple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.edu.agh.ietanks.engine.api.Action;
import pl.edu.agh.ietanks.engine.api.Board;
import pl.edu.agh.ietanks.engine.api.Missile;
import pl.edu.agh.ietanks.engine.api.MutableBoard;
import pl.edu.agh.ietanks.engine.api.Position;
import pl.edu.agh.ietanks.engine.api.events.Event;
import pl.edu.agh.ietanks.engine.api.events.MissileCreated;
import pl.edu.agh.ietanks.engine.api.events.MissileDestroyed;
import pl.edu.agh.ietanks.engine.api.events.MissileMoved;
import pl.edu.agh.ietanks.engine.api.events.TankDestroyed;
import pl.edu.agh.ietanks.engine.api.events.TankMoved;
import pl.edu.agh.ietanks.engine.simple.actions.Move;
import pl.edu.agh.ietanks.engine.simple.actions.Shot;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class GameLogic {
    private MutableBoard board;

    public GameLogic(MutableBoard board) {
        this.board = board;
    }

    public List<Event> moveMissiles() {
    	List<Event> events = new ArrayList<>();
    	
    	// first move all missiles
    	List<Missile> missilesNotWithinBoard = new ArrayList<Missile>();
    	for (Missile missile : board.findMissiles()) {
    		Position destination = findMissileDestination(missile.direction(), missile.position(), missile.speed());
    		Preconditions.checkNotNull(destination, "invalid direction");
    		if (board.isWithin(destination)) {
    			board.replaceMissile(missile, destination);
                events.add(new MissileMoved(destination, missile.direction(), missile.speed()));
    		}
    		else {
    			missilesNotWithinBoard.add(missile);
    		}
    		
    	}
    	
    	// then remove missiles not within board
    	for (Missile missile : missilesNotWithinBoard) {
    		board.removeMissile(missile);
            events.add(new MissileDestroyed(missile.position(), missile.direction(), missile.speed()));
    	}
    	
    	Map<Position, List<Missile>> missilePositions = new HashMap<>();
    	for (Missile missile : board.findMissiles()) {
    		if (!missilePositions.containsKey(missile.position())) {
    			missilePositions.put(missile.position(), new ArrayList<Missile>());
    		}
    		missilePositions.get(missile.position()).add(missile);
    	}
    	
    	// then remove missiles at the same positions
    	for (Position position : missilePositions.keySet()) {
            if (missilePositions.get(position).size() > 1) {
            	for (Missile missile : missilePositions.get(position)) {
            		board.removeMissile(missile);
            		events.add(new MissileDestroyed(missile.position(), missile.direction(), missile.speed()));
            	}
            	missilePositions.remove(position);
            }
    	}
    	
    	// then remove missiles and tanks at the same position
    	for (Position position : missilePositions.keySet()) {
    		if (board.findTank(position) != null) {
    			
    			int tankId = board.findTank(position);
    			board.removeTank(tankId);
    			events.add(new TankDestroyed(tankId));
    			
    			Missile missile = missilePositions.get(position).get(0);
        		board.removeMissile(missile);
        		events.add(new MissileDestroyed(missile.position(), missile.direction(), missile.speed()));
    		}
    	}
    	
    	
    	return events;
    }



    public Board board() {
        return board;
    }

    public List<Event> tryApplyAction(Action proposedAction, int botId) {
        List<Event> events = new ArrayList<>();

        Optional<Position> botPosition = board.findTank(botId);

        if(!botPosition.isPresent()) {
            return events;
        }

        if(proposedAction instanceof Move) {
        	Move move = (Move) proposedAction;
            Position destination = findMoveDestination(botPosition, move);
            Preconditions.checkNotNull(destination, "invalid movement");
            if(board.isWithin(destination) && board.isAccessibleForTank(destination)) {
                board.replaceTank(botId, destination);
                events.add(new TankMoved(botId, move.getDirection(), move.getStep()));
            }
            // check if there are missiles here
            Collection<Missile> missiles = board.findMissiles(destination);
            if (!missiles.isEmpty()) {
            	board.removeTank(botId);
            	events.add(new TankDestroyed(botId));
            	for (Missile missile : missiles) {
            		board.removeMissile(missile);
            		events.add(new MissileDestroyed(missile.position(), missile.direction(), missile.speed()));
            	}
            }
            
        }
        else if(proposedAction instanceof Shot) {
        	Shot shot = (Shot) proposedAction;
        	Position destination = findMissileDestination(shot.getDirection(), botPosition.get(), shot.getSpeed());
        	
        	// create missile
        	Missile missile = new Missile(shot.getSpeed(), shot.getDirection(), destination);
            board.createMissile(missile);
            events.add(new MissileCreated(destination, shot.getDirection(), shot.getSpeed()));
            
            // remove missiles at the same positions
            if (board.findMissiles(destination).size() > 1) {
        		
        		Collection<Missile> missilesToDestroy = board.findMissiles(destination);
        		for (Missile missileToDestroy : missilesToDestroy) {
        			board.removeMissile(missileToDestroy);
            		events.add(new MissileDestroyed(
            				missileToDestroy.position(), missileToDestroy.direction(), missileToDestroy.speed()));
        		}
            }
            
            // remove tank at the same position
            if (board.findTank(destination) != null) {
    			int tankId = board.findTank(destination);
    			board.removeTank(tankId);
    			events.add(new TankDestroyed(tankId));
    			
        		board.removeMissile(missile);
        		events.add(new MissileDestroyed(missile.position(), missile.direction(), missile.speed()));
            }
        }

        return events;
    }
    
	private Position findMissileDestination(Board.Direction direction, Position position, int speed) {
		Position destination = null;
		if (direction == Board.Direction.Right) {
			destination = position.toRight(speed);
		}
		else if (direction == Board.Direction.Left) {
			destination = position.toLeft(speed);
		}
		else if (direction == Board.Direction.Up) {
			destination = position.toUp(speed);
		}
		else if (direction == Board.Direction.Down) {
			destination = position.toDown(speed);
		}
		else if (direction == Board.Direction.Up_Left) {
			destination = position.toUpLeft(speed);
		}
		else if (direction == Board.Direction.Up_Right) {
			destination = position.toUpRight(speed);
		}
		else if (direction == Board.Direction.Down_Left) {
			destination = position.toDownLeft(speed);
		}
		else if (direction == Board.Direction.Down_Right) {
			destination = position.toDownRight(speed);
		}
		return destination;
	}

	private Position findMoveDestination(Optional<Position> botPosition,
			Move move) {
		Position destination = null;
		
		if (move.getDirection() == Board.Direction.Right) {
			destination = botPosition.get().toRight(move.getStep());
		}
		else if (move.getDirection() == Board.Direction.Left) {
			destination = botPosition.get().toLeft(move.getStep());
		}
		else if (move.getDirection() == Board.Direction.Up) {
			destination = botPosition.get().toUp(move.getStep());
		}
		else if (move.getDirection() == Board.Direction.Down){
			destination = botPosition.get().toDown(move.getStep());
		}
		return destination;
	}

}
