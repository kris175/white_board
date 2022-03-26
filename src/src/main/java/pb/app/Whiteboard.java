package pb.app;

import java.util.ArrayList;
import java.util.logging.Logger;

import pb.managers.PeerManager;
import pb.managers.endpoint.Endpoint;

/**
 * Class to maintain whiteboard information. You should probably modify this
 * class.
 * @author aaron
 *
 */
public class Whiteboard {
	private static Logger log = Logger.getLogger(Whiteboard.class.getName());

	/**
	 * Paths for this whiteboard.
	 */
	private ArrayList<WhiteboardPath> paths;

	/**
	 * Name of the whiteboard, peer:port:boarid
	 */
	private String name;

	/**
	 * The current version number of this whiteboard.
	 */
	private long version;

	/**
	 * Whether this whiteboard is being shared or not. Only relevant
	 * for boards that are created locally.
	 */
	private boolean shared=false;

	/**
	 * Whether this whiteboard is a remote board, i.e. not created
	 * locally but rather being managed on another peer.
	 */
	private boolean remote=false;

	/**
	 * Initialize the whiteboard.
	 * @param remote is true if the whiteboard is remotely managed, otherwise
	 * the whiteboard is locally managed.
	 */
	public Whiteboard(String name,boolean remote) {
		paths = new ArrayList<>();
		this.name=name;
		this.version=0;
		this.remote=remote;
	}

	/**
	 * Initialize a whiteboard from a string.
	 *
	 * @param name the board name, i.e. peer:port:boardid
	 * @param data the board data, i.e. version%PATHS 
	 */
	public void whiteboardFromString(String name,String data) {
		String[] parts = data.split("%");
		paths = new ArrayList<>();
		this.name=name;
		version=-1;
		if(parts.length<1) {
			log.severe("whiteboard data is malformed: "+data);
			return;
		}
		try {
			version=Integer.parseInt(parts[0]);
		} catch (NumberFormatException e) {
			log.severe("whiteboard data is malformed: "+data);
			return;
		}
		if(parts.length>1) {
			for (int i = 1; i < parts.length; i++) {
				String path = parts[i];
				if (path.length() > 0) {
					paths.add(new WhiteboardPath(path));
				}
			}
		}
	}


	/**
	 * Convert this whiteboard to a string.
	 *
	 * @return "name%version%" if the whiteboard has no paths or
	 *         "name%version%PATHS" for the case when there are one or more paths,
	 *         where each path is separated by a "%"
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder("");
		sb.append(getNameAndVersion());
		if(paths.size()==0)
			sb.append("%");
		else {
			for (int i = 0; i < paths.size(); i++) {
				sb.append("%"+paths.get(i));
			}
		}
		return sb.toString();
	}

	/**
	 * Draw the white board on the drawing area. Clears the draw
	 * area and draws all paths.
	 * @param drawArea
	 */
	public void draw(DrawArea drawArea) {
		drawArea.clear();
		for(WhiteboardPath path : paths) {
			drawArea.drawPath(path);
		}
	}

	////
	// Methods that update the version of the board
	////


	/**
	 * SUBSCRIBE TO HOST
	 * Connect this whiteboard with the peers that have selected this board once shared.
	 * Keep track of whiteboard peers that are listenting to this board
	 */

	private static ArrayList<Endpoint> subscribers = new ArrayList<Endpoint>();

	public void subscribeToHost(Endpoint endpoint){
		subscribers.add(endpoint);
		// Listen on the subscriber endpoint for any events
	}

	/**
	 * PUBLISH TO HOST
	 * 1. Add host endpoint to publish to it
	 * 2. Publish to host if the board is remote whenever the addPath() is called
	 */
	private Endpoint hostEndpoint;
	public void addHostEndpoint(Endpoint endpoint){
		hostEndpoint = endpoint;
	}


	/**
	 * PUBLISH TO SUBSCRIBERS
	 * boardId:version:path
	 */
	private void publishPathUpdate(WhiteboardPath newPath){
		String data = name+"%"+version+"%"+newPath.toString();

		for(Endpoint subscriber: subscribers){
			subscriber.emit(WhiteboardApp.boardPathAccepted, data);
		}
	}

	/**
	 * LISTEN TO SUBSCRIBERS
	 */


	/**
	 * Add a path to the whiteboard.
	 * @param newPath
	 * @param versionBeingUpdated should be the board version that the update applies to
	 * @return true if the update was accepted, false if it was rejected
	 */
	private boolean remoteUpdated = false;

	public synchronized boolean addPath(WhiteboardPath newPath,long versionBeingUpdated) {
		if(version!=versionBeingUpdated) return false;
		paths.add(newPath);

		if (shared) {
			// HOST
			publishPathUpdate(newPath);
			this.version++;
		} else if (remote){
			// CLIENT
			String data = name+"%"+version+"%"+newPath.toString();
			remoteUpdated = true;
			this.version++;
			hostEndpoint.emit(WhiteboardApp.boardPathUpdate,data);
		} else {
			this.version++;
		}

		return true;
	}

	// Server side
	public synchronized boolean addPathFromClient(WhiteboardPath newPath,long versionBeingUpdated) {
		if(version!=versionBeingUpdated) return false;
		paths.add(newPath);
		publishPathUpdate(newPath);
		this.version++;
		return true;
	}

	// Peer side
	public synchronized boolean addPathFromHost(WhiteboardPath newPath,long versionBeingUpdated) {
		if(remoteUpdated){
			remoteUpdated = false;
			return true;
		}
		if(version!=versionBeingUpdated) return false;
		paths.add(newPath);
		this.version++;
		return true;
	}

	/**
	 * Clear the board of all paths.
	 * @param versionBeingUpdated should be the board version that the update applies to
	 * @return true if the update was accepted, false if it was rejected
	 */

	/**
	 *
	 */
	private void publishClearUpdate(){
		for(Endpoint subscriber: subscribers){
			subscriber.emit(WhiteboardApp.boardClearAccepted, getNameAndVersion());
		}
	}

	public synchronized boolean clear(long versionBeingUpdated) {
		if(version!=versionBeingUpdated) return false;
		paths.clear();

		if (shared) {
			publishClearUpdate();
			this.version++;
		} else if (remote){
			String data = getNameAndVersion();
			remoteUpdated = true;
			this.version++;
			hostEndpoint.emit(WhiteboardApp.boardClearUpdate,data);
		} else {
			this.version++;
		}

		return true;
	}

	// Host side

	/**
	 * Receive CLEAR_BOARD on HOST from PEER
	 * HOST emits CLEAR_BOARD event to all its peers
	 * @param versionBeingUpdated
	 * @return
	 */
	public synchronized boolean clearFromPeer(long versionBeingUpdated) {
		if(version!=versionBeingUpdated) return false;
		paths.clear();
		publishClearUpdate();
		this.version++;
		return true;
	}

	// Peer side

	/**
	 * Receive CLEAR_BOARD on PEER from HOST
	 * @param versionBeingUpdated
	 * @return
	 */
	public synchronized boolean clearFromHost(long versionBeingUpdated) {
		if(remoteUpdated){
			remoteUpdated = false;
			return true;
		}
		if(version!=versionBeingUpdated) return false;
		paths.clear();
		this.version++;
		return true;
	}

	/**
	 * Remove the last path from the board.
	 */
	private void publishUndoUpdate(){
		for(Endpoint subscriber: subscribers){
			subscriber.emit(WhiteboardApp.boardUndoAccepted, getNameAndVersion());
		}
	}

	public synchronized boolean undo(long versionBeingUpdated) {
		if(version!=versionBeingUpdated) return false;

		if(paths.size()>0) {
			paths.remove(paths.size()-1);
		}

		if (shared) {
			publishUndoUpdate();
			this.version++;
		} else if (remote){
			String data = getNameAndVersion();
			remoteUpdated = true;
			this.version++;
			hostEndpoint.emit(WhiteboardApp.boardUndoUpdate,data);
		} else {
			this.version++;
		}

		return true;
	}

	// Host side

	/**
	 * Receive BOARD_UNDO_UPDATE on HOST from PEER
	 * HOST emits CLEAR_BOARD event to all its peers
	 * @param versionBeingUpdated
	 * @return
	 */
	public synchronized boolean undoFromPeer(long versionBeingUpdated) {
		if(version!=versionBeingUpdated) return false;
		if(paths.size()>0) {
			paths.remove(paths.size()-1);
		}
		publishUndoUpdate();
		this.version++;
		return true;
	}

	// Peer side

	/**
	 * Receive BOARD_UNDO_ACCEPTED on PEER from HOST
	 * @param versionBeingUpdated
	 * @return
	 */

	public synchronized boolean undoFromHost(long versionBeingUpdated) {
		if(remoteUpdated){
			remoteUpdated = false;
			return true;
		}
		if(version!=versionBeingUpdated) return false;
		if(paths.size()>0) {
			paths.remove(paths.size()-1);
		}
		this.version++;
		return true;
	}

	/**
	 * BOARD_DELETE
	 * @return
	 */

	private void publishDeleteUpdate(){
		for(Endpoint subscriber: subscribers){
			subscriber.emit(WhiteboardApp.boardDeleted, getName());
		}
	}

	public void delete(){
		if(shared){
			publishDeleteUpdate();
		} else if (remote){
			hostEndpoint.emit(WhiteboardApp.unlistenBoard, getName());
		}
	}

	public Endpoint getHostEndpoint(){
		return hostEndpoint;
	}

	public void unsubscribe(Endpoint endpoint){
		subscribers.remove(endpoint);
	}

	/**
	 *
	 * @return peer:port:boardid%version
	 */
	public String getNameAndVersion() {
		return getName()+"%"+getVersion();
	}

	/**
	 *
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 *
	 * @return true if the board is shared, false otherwise
	 */
	public boolean isShared() {
		return shared;
	}

	/**
	 * Set the shared status of the board
	 * @param shared
	 */
	public void setShared(boolean shared) {
		this.shared=shared;
	}

	/**
	 *
	 * @return the version of the board
	 */
	public long getVersion() {
		return version;
	}

	/**
	 *
	 * @return whether the board is maintained remotely or not
	 */
	public boolean isRemote() {
		return remote;
	}

}
