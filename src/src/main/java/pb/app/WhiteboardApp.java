package pb.app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.net.UnknownHostException;
import pb.managers.ClientManager;
import pb.managers.IOThread;
import pb.managers.PeerManager;
import pb.managers.ServerManager;
import pb.managers.endpoint.Endpoint;
import pb.WhiteboardServer;
import pb.utils.Utils;
import java.util.*;

/**
 * Initial code obtained from:
 * https://www.ssaurel.com/blog/learn-how-to-make-a-swing-painting-and-drawing-application/
 */
public class WhiteboardApp {
	private static Logger log = Logger.getLogger(WhiteboardApp.class.getName());

	/**
	 * Emitted to another peer to subscribe to updates for the given board. Argument
	 * must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String listenBoard = "BOARD_LISTEN";

	/**
	 * Emitted to another peer to unsubscribe to updates for the given board.
	 * Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String unlistenBoard = "BOARD_UNLISTEN";

	/**
	 * Emitted to another peer to get the entire board data for a given board.
	 * Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String getBoardData = "GET_BOARD_DATA";

	/**
	 * Emitted to another peer to give the entire board data for a given board.
	 * Argument must have format "host:port:boardid%version%PATHS".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardData = "BOARD_DATA";

	/**
	 * Emitted to another peer to add a path to a board managed by that peer.
	 * Argument must have format "host:port:boardid%version%PATH". The numeric value
	 * of version must be equal to the version of the board without the PATH added,
	 * i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardPathUpdate = "BOARD_PATH_UPDATE";

	/**
	 * Emitted to another peer to indicate a new path has been accepted. Argument
	 * must have format "host:port:boardid%version%PATH". The numeric value of
	 * version must be equal to the version of the board without the PATH added,
	 * i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardPathAccepted = "BOARD_PATH_ACCEPTED";

	/**
	 * Emitted to another peer to remove the last path on a board managed by that
	 * peer. Argument must have format "host:port:boardid%version%". The numeric
	 * value of version must be equal to the version of the board without the undo
	 * applied, i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardUndoUpdate = "BOARD_UNDO_UPDATE";

	/**
	 * Emitted to another peer to indicate an undo has been accepted. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the undo applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardUndoAccepted = "BOARD_UNDO_ACCEPTED";

	/**
	 * Emitted to another peer to clear a board managed by that peer. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the clear applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardClearUpdate = "BOARD_CLEAR_UPDATE";

	/**
	 * Emitted to another peer to indicate an clear has been accepted. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the clear applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardClearAccepted = "BOARD_CLEAR_ACCEPTED";

	/**
	 * Emitted to another peer to indicate a board no longer exists and should be
	 * deleted. Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardDeleted = "BOARD_DELETED";

	/**
	 * Emitted to another peer to indicate an error has occurred.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardError = "BOARD_ERROR";

	/**
	 * White board map from board name to board object
	 */
	Map<String,Whiteboard> whiteboards;

	/**
	 * The currently selected white board
	 */
	Whiteboard selectedBoard = null;

	/**
	 * The peer:port string of the peer. This is synonomous with IP:port, host:port,
	 * etc. where it may appear in comments.
	 */
	String peerport="standalone"; // a default value for the non-distributed version
	String clientPort = "";
	/*
	 * GUI objects, you probably don't need to modify these things... you don't
	 * need to modify these things... don't modify these things [LOTR reference?].
	 */

	JButton clearBtn, blackBtn, redBtn, createBoardBtn, deleteBoardBtn, undoBtn;
	JCheckBox sharedCheckbox ;
	DrawArea drawArea;
	JComboBox<String> boardComboBox;
	boolean modifyingComboBox=false;
	boolean modifyingCheckBox=false;

	private static PeerManager peerManager;

	/**
	 * Initialize the white board app.
	 */
	public WhiteboardApp(int peerPort,String whiteboardServerHost,
						 int whiteboardServerPort) {
		whiteboards=new HashMap<>();

		acceptPeerConnections(peerPort,whiteboardServerHost,whiteboardServerPort);
	}

	public void acceptPeerConnections(int peerPort,String whiteboardServerHost,
									  int whiteboardServerPort){

		peerManager = new PeerManager(peerPort);

		peerManager.on(PeerManager.peerStarted, (args)->{

			Endpoint endpoint = (Endpoint)args[0];

			endpoint.on(listenBoard, args2 ->{

				String boardId = (String)args2[0];
				onListenBoard(endpoint,boardId);

			}).on(getBoardData, args2 ->{

				String boardId = (String)args2[0];
				onGetBoardData(endpoint, boardId);

			}).on(boardClearUpdate, args2->{

				String boardName = getBoardName((String)args2[0]);
				Whiteboard hostBoard = whiteboards.get(boardName);
				onClearBoardUpdateFromClient((String)args2[0],hostBoard, endpoint);

			}).on(boardUndoUpdate, args2 ->{

				String boardName = getBoardName((String)args2[0]);
				Whiteboard hostBoard = whiteboards.get(boardName);
				onUndoUpdateFromClient((String)args2[0],hostBoard, endpoint);

			}).on(boardPathUpdate,args2->{

				String boardName = getBoardName((String)args2[0]);
				Whiteboard hostBoard = whiteboards.get(boardName);
				onBoardPathUpdateFromClient((String)args2[0],hostBoard, endpoint);

			}).on(unlistenBoard, args2 -> {
				String boardName = getBoardName((String)args2[0]);
				onUnlistenFromClient(boardName,endpoint);
			}).on(boardError, args2 ->{
				log.info((String)args2[0]);
			});

		}).on(PeerManager.peerStopped,(args)->{
			Endpoint endpoint = (Endpoint)args[0];
			log.info("Disconnected from peer: "+endpoint.getOtherEndpointId());
		}).on(PeerManager.peerError,(args)->{
			Endpoint endpoint = (Endpoint)args[0];
			log.info("There was an error communicating with the peer: "
					+endpoint.getOtherEndpointId());
		}).on(PeerManager.peerServerManager, (args)->{
			ServerManager serverManager = (ServerManager)args[0];
			serverManager.on(IOThread.ioThread, (args2)->{
				peerport = (String) args2[0];
				show(peerport);
				connectToServer(whiteboardServerHost,whiteboardServerPort);
			});
		});

		peerManager.start();

	}

	private static ClientManager whiteboardServerManager;
	private static Endpoint whiteboardServerEndpoint;
	private boolean connectedToServer = false;

	// Make connection to the server and route events to the correct shared whiteboard
	public void connectToServer(String whiteboardServerHost,
								int whiteboardServerPort){
		try{
			whiteboardServerManager = peerManager.connect(whiteboardServerPort, whiteboardServerHost);
			whiteboardServerManager.on(PeerManager.peerStarted, (args)->{
				
				whiteboardServerEndpoint = (Endpoint)args[0];

				whiteboardServerEndpoint.on(WhiteboardServer.sharingBoard, (args2) -> {

					String sharedBoardName = (String) args2[0];
					onSharingBoard(sharedBoardName);

				}).on(WhiteboardServer.unsharingBoard, (args3)->{

					String boardname = (String) args3[0];
					deleteBoardFromHost(boardname);

				});

				log.info("Connected to whiteboard server: "+whiteboardServerEndpoint.getOtherEndpointId());
				connectedToServer = true;

			}).on(PeerManager.peerStopped, (args)->{

				Endpoint endpoint = (Endpoint)args[0];
				log.info("Disconnected from the whiteboard server: "+endpoint.getOtherEndpointId());

			}).on(PeerManager.peerError, (args)->{

				Endpoint endpoint = (Endpoint)args[0];
				log.info("There was an error communicating with the whiteboard server: "
						+endpoint.getOtherEndpointId());
				connectedToServer = false;

			});

			whiteboardServerManager.start();

		}catch(Exception e){
			log.info("error");
		}
	}

	/**
	 * Establish connection to the sharedBoad Peer (HOST) whenever a board is shared.
	 * This peer will connect to multiple host boards upon receiving SHARE_BOARD on unique endpoints
	 */


	private static Map<String,ClientManager>  remoteManagers = new HashMap<>();

	public void onSharingBoard(String sharedBoardName){

		try{
			ClientManager clientManager = peerManager.connect(getPort(sharedBoardName),getIP(sharedBoardName));
			clientManager.on(PeerManager.peerStarted, (args)->{

				remoteManagers.put(sharedBoardName,clientManager);

				Endpoint endpoint = (Endpoint)args[0];


				Whiteboard whiteboard = new Whiteboard(sharedBoardName,true);

				addBoard(whiteboard,false);
				whiteboard.addHostEndpoint(endpoint);

				// On receiving the data and initialising the board, listen to it
				endpoint.on(boardData, args2 -> {
					log.info("Board Data"+(String)args2[0]);
					onBoardData(whiteboard,sharedBoardName,(String)args2[0]);
					endpoint.emit(listenBoard,sharedBoardName);
					log.info("Listen Board: " + sharedBoardName);
				});

				// Get notified that a path has been accepted by the host, so added it
				endpoint.on(boardPathAccepted, args2 -> {
					onBoardPathUpdate((String)args2[0],whiteboard);
				});

				// Get notified that the board cleared accepted by the host, so clear it
				endpoint.on(boardClearAccepted, args2 -> {
					onClearBoardUpdate((String)args2[0],whiteboard);
				});

				// Get notified undo accepted, so undo!
				endpoint.on(boardUndoAccepted, args2 -> {
					onUndoUpdate((String)args2[0],whiteboard);
				});

				// Delete board accepted on the host
				endpoint.on(boardDeleted, args2 -> {
					deleteBoardFromHost((String)args2[0]);
				});

				// Get the board data from the host
				endpoint.emit(getBoardData,sharedBoardName);
				log.info("Get Board Data"+sharedBoardName);
			}).on(PeerManager.peerStopped,(args)->{
				Endpoint endpoint = (Endpoint)args[0];
				log.info("Disconnected from peer: "+endpoint.getOtherEndpointId());
			}).on(PeerManager.peerError,(args)->{
				Endpoint endpoint = (Endpoint)args[0];
				log.info("There was an error communicating with the peer: "
						+endpoint.getOtherEndpointId());
			});
			clientManager.start();
		} catch (Exception e){
			log.info("error");
		}

	}

	/******
	 *
	 * Utility methods to extract fields from argument strings.
	 *
	 ******/

	/**
	 *
	 * @param data = peer:port:boardid%version%PATHS
	 * @return peer:port:boardid
	 */
	public static String getBoardName(String data) {
		String[] parts=data.split("%",2);
		return parts[0];
	}

	/**
	 *
	 * @param data = peer:port:boardid%version%PATHS
	 * @return boardid
	 */
	public static String getBoardId(String data){
		String[] parts = getBoardIdAndData(data).split("%",3);
		return parts[0];
	}

	/**
	 *
	 * @param data = peer:port:boardid%version%PATHS
	 * @return boardid%version%PATHS
	 */
	public static String getBoardIdAndData(String data) {
		String[] parts=data.split(":");
		return parts[2];
	}

	/**
	 *
	 * @param data = peer:port:boardid%version%PATHS
	 * @return version%PATHS
	 */
	public static String getBoardData(String data) {
		String[] parts=data.split("%",2);
		return parts[1];
	}

	/**
	 *
	 * @param data = peer:port:boardid%version%PATHS
	 * @return version
	 */
	public static long getBoardVersion(String data) {
		String[] parts=data.split("%",3);
		return Long.parseLong(parts[1]);
	}

	/**
	 *
	 * @param data = peer:port:boardid%version%PATHS
	 * @return PATHS
	 */
	public static String getBoardPaths(String data) {
		String[] parts=data.split("%",3);
		return parts[2];
	}

	/**
	 *
	 * @param data = peer:port:boardid%version%PATHS
	 * @return peer
	 */
	public static String getIP(String data) {
		String[] parts=data.split(":");
		return parts[0];
	}

	/**
	 *
	 * @param data = peer:port:boardid%version%PATHS
	 * @return port
	 */
	public static int getPort(String data) {
		String[] parts=data.split(":");
		return Integer.parseInt(parts[1]);
	}

	/******
	 *
	 * Methods called from events.
	 *
	 ******/

	// ------------------------------ From whiteboard server ------------------------------

	public void onBoardData(Whiteboard whiteboard, String sharedBoardName, String data){
		whiteboard.whiteboardFromString(sharedBoardName,getBoardData(data));
	}

	/**
	 * BOARD_PATH_ACCEPTED
	 * @param eventArgs
	 * @param whiteboard
	 */
	public void onBoardPathUpdate(String eventArgs, Whiteboard whiteboard){

		if (getBoardName(eventArgs).equals(whiteboard.getName())){
			WhiteboardPath updatedPath = new WhiteboardPath(getBoardPaths(eventArgs));
			if(!whiteboard.addPathFromHost(updatedPath, getBoardVersion(eventArgs))){
				whiteboard.getHostEndpoint().emit(boardError,"Version mismatch");
			}
			if (selectedBoard.getName().equals(whiteboard.getName())){
				drawArea.clear();
				whiteboard.draw(drawArea);
				log.info(updatedPath.toString());
			}
		}

	}

	/**
	 * BOARD_CLEAR_ACCEPTED
	 */
	public void onClearBoardUpdate(String eventArgs, Whiteboard whiteboard){
		if (getBoardName(eventArgs).equals(whiteboard.getName())){
			if(!whiteboard.clearFromHost(getBoardVersion(eventArgs))){
				whiteboard.getHostEndpoint().emit(boardError,"Version mismatch");
			}else{
				if (selectedBoard.getName().equals(whiteboard.getName())){
					drawArea.clear();
					whiteboard.draw(drawArea);
					log.info("Clear Board"+whiteboard.getName());
				}
			}
		}
	}

	/**
	 * BOARD_UNDO_ACCEPTED
	 */
	public void onUndoUpdate(String eventArgs, Whiteboard whiteboard){
		if (getBoardName(eventArgs).equals(whiteboard.getName())){
			if(!whiteboard.undoFromHost(getBoardVersion(eventArgs))){
				whiteboard.getHostEndpoint().emit(boardError,"Version mismatch");
			}else{
				if (selectedBoard.getName().equals(whiteboard.getName())){
					drawArea.clear();
					whiteboard.draw(drawArea);
					log.info("Undo Board"+whiteboard.getName());
				}
			}
		}
	}

	/**
	 * BOARD UNLISTEN
	 */

	public void onUnlistenFromClient(String boardName, Endpoint endpoint){
		Whiteboard hostBoard = whiteboards.get(boardName);
		hostBoard.unsubscribe(endpoint);
		log.info("Receive Client Unlisten"+hostBoard .getName());
	}

	// ------------------------------ From whiteboard peer ------------------------------

	/**
	 * BOARD_PATH
	 * @param eventArgs
	 * @param whiteboard
	 */
	public void onBoardPathUpdateFromClient(String eventArgs, Whiteboard whiteboard, Endpoint endpoint){
		if (getBoardName(eventArgs).equals(whiteboard.getName())){
			WhiteboardPath updatedPath = new WhiteboardPath(getBoardPaths(eventArgs));
			if(!whiteboard.addPathFromClient(updatedPath, getBoardVersion(eventArgs))){
				endpoint.emit(boardError,"A peer board that attempted to draw has version mismatch. Path not updated.");
			}
			if (selectedBoard.getName().equals(whiteboard.getName())){
				drawArea.clear();
				whiteboard.draw(drawArea);
				log.info(updatedPath.toString());
			}
		}
	}

	/**
	 * BOARD_CLEAR
	 * hmm, dont actually need to check if it is the correct board, its already been done
	 * when the hostBoard was selected
	 */
	public void onClearBoardUpdateFromClient(String eventArgs, Whiteboard whiteboard, Endpoint endpoint){

		if (getBoardName(eventArgs).equals(whiteboard.getName())){
			if(!whiteboard.clearFromPeer(getBoardVersion(eventArgs))){
				endpoint.emit(boardError,"A peer board that attempted to clear has version mismatch. Board not cleared.");
			}else{
				if (selectedBoard.getName().equals(whiteboard.getName())){
					drawArea.clear();
					whiteboard.draw(drawArea);
					log.info("Clear Board Accepted"+ whiteboard.getName());
				}
			}
		}
	}

	/**
	 * BOARD_UNDO_ACCEPTED
	 */
	public void onUndoUpdateFromClient(String eventArgs, Whiteboard whiteboard, Endpoint endpoint){
		if (getBoardName(eventArgs).equals(whiteboard.getName())){
			if(!whiteboard.undoFromPeer(getBoardVersion(eventArgs))){
				endpoint.emit(boardError,"A peer board that attempted to undo has version mismatch. Undo not accepted.");
			}else{
				if (selectedBoard.getName().equals(whiteboard.getName())){
					drawArea.clear();
					whiteboard.draw(drawArea);
					log.info("Undo Update Accepted"+ whiteboard.getName());
				}
			}
		}
	}

	/**
	 * Other stuff, probably belongs right above, move it later on!
	 * @param endpoint
	 * @param sharedBoard
	 */

	public void onGetBoardData(Endpoint endpoint, String sharedBoard){
		endpoint.emit(boardData,whiteboards.get(sharedBoard).toString());
	}

	public void onListenBoard(Endpoint remoteEndpoint, String remoteBoardName){
		Whiteboard hostBoard = whiteboards.get(remoteBoardName);
		hostBoard.subscribeToHost(remoteEndpoint);
	}



	public void shareBoard(Whiteboard selectedBoard){
		whiteboardServerEndpoint.emit(WhiteboardServer.shareBoard,selectedBoard.getName());
		log.info("Share board"+selectedBoard.getName());
	}
	public void unshareBoard(Whiteboard selectedBoard){
		whiteboardServerEndpoint.emit(WhiteboardServer.unshareBoard,selectedBoard.getName());
		log.info("Unshare board"+selectedBoard.getName());
	}


	/******
	 *
	 * Methods to manipulate data locally. Distributed systems related code has been
	 * cut from these methods.
	 *
	 ******/

	/**
	 * Wait for the peer manager to finish all threads.
	 */
	public void waitToFinish() {
		try{
			peerManager.join();
			peerManager.getServerManager().join();
			peerManager.joinWithClientManagers();
		}catch(Exception e){log.info("exception");
		}

	}

	/**
	 * Add a board to the list that the user can select from. If select is
	 * true then also select this board.
	 * @param whiteboard
	 * @param select
	 */
	public void addBoard(Whiteboard whiteboard,boolean select) {
		synchronized(whiteboards) {
			whiteboards.put(whiteboard.getName(), whiteboard);
		}
		updateComboBox(select?whiteboard.getName():null);
	}

	/**
	 * Delete a board from the list.
	 * @param boardname must have the form peer:port:boardid
	 */
	public void deleteBoard(String boardname) {
		synchronized(whiteboards) {
			Whiteboard whiteboard = whiteboards.get(boardname);
			if(whiteboard!=null) {
				whiteboards.remove(boardname);
				log.info("Delete Board"+boardname);
				if(whiteboard.isShared()){
					whiteboard.delete();
					whiteboardServerEndpoint.emit(WhiteboardServer.unshareBoard, whiteboard.getName());
				} else if (whiteboard.isRemote()){
					whiteboard.delete();
					ClientManager clientManager = remoteManagers.get(boardname);
					clientManager.shutdown();
				}
			}
		}
		updateComboBox(null);
	}

	public void deleteBoardFromHost(String boardname) {
		synchronized(whiteboards) {
			Whiteboard whiteboard = whiteboards.get(boardname);
			if(whiteboard!=null) {
				whiteboards.remove(boardname);
				ClientManager clientManager = remoteManagers.get(boardname);
				clientManager.shutdown();
				log.info("Board deleted" + whiteboard.getName());
			}
		}
		updateComboBox(null);
	}

	/**
	 * Create a new local board with name peer:port:boardid.
	 * The boardid includes the time stamp that the board was created at.
	 */
	public void createBoard() {
		String name = peerport+":board"+Instant.now().toEpochMilli();
		Whiteboard whiteboard = new Whiteboard(name,false);
		addBoard(whiteboard,true);
	}

	/**
	 * Add a path to the selected board. The path has already
	 * been drawn on the draw area; so if it can't be accepted then
	 * the board needs to be redrawn without it.
	 * @param currentPath
	 */
	public void pathCreatedLocally(WhiteboardPath currentPath) {
		if(selectedBoard!=null) {
			System.out.println("----------------Draw-------------------");
			if(!selectedBoard.addPath(currentPath,selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard(); // just redraw the screen without the path
			} else {
				// was accepted locally, so do remote stuff if needed

			}
		} else {
			log.severe("path created without a selected board: "+currentPath);
		}
	}

	/**
	 * Clear the selected whiteboard.
	 */
	public void clearedLocally() {
		if(selectedBoard!=null) {
			if(!selectedBoard.clear(selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard();
			} else {
				// was accepted locally, so do remote stuff if needed

				drawSelectedWhiteboard();
			}
		} else {
			log.severe("cleared without a selected board");
		}
	}

	/**
	 * Undo the last path of the selected whiteboard.
	 */
	public void undoLocally() {
		if(selectedBoard!=null) {
			if(!selectedBoard.undo(selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard();
			} else {
				drawSelectedWhiteboard();
			}
		} else {
			log.severe("undo without a selected board");
		}
	}

	/**
	 * The variable selectedBoard has been set.
	 */
	public void selectedABoard() {

		drawSelectedWhiteboard();
		log.info("selected board: "+selectedBoard.getName());
		/**
		 * Upon selecting the board in the gui,
		 * check if it is remote
		 * if (remote):
		 * connect to the remote board and get data from it.
		 */
	}

	/**
	 * Set the share status on the selected board.
	 */
	public void setShare(boolean share) {
		if(selectedBoard!=null) {
			selectedBoard.setShared(share);
			if (selectedBoard.isShared()){
				shareBoard(selectedBoard);
			}else{
				unshareBoard(selectedBoard);
			}
			System.out.println("Sharing the board");
		} else {
			log.severe("there is no selected board");
		}
	}

	/**
	 * Called by the gui when the user closes the app.
	 */
	public void guiShutdown() {
		// do some final cleanup
		HashSet<Whiteboard> existingBoards= new HashSet<>(whiteboards.values());
		existingBoards.forEach((board)->{
			deleteBoard(board.getName());
		});
		peerManager.shutdown();
	}



	/******
	 *
	 * GUI methods and callbacks from GUI for user actions.
	 * You probably do not need to modify anything below here.
	 *
	 ******/

	/**
	 * Redraw the screen with the selected board
	 */
	public void drawSelectedWhiteboard() {
		drawArea.clear();
		if(selectedBoard!=null) {
//			System.out.println("----------------Draw-------------------");
			selectedBoard.draw(drawArea);
		}
	}

	/**
	 * Setup the Swing components and start the Swing thread, given the
	 * peer's specific information, i.e. peer:port string.
	 */
	public void show(String peerport) {
		// create main frame
		JFrame frame = new JFrame("Whiteboard Peer: "+peerport);
		Container content = frame.getContentPane();
		// set layout on content pane
		content.setLayout(new BorderLayout());
		// create draw area
		drawArea = new DrawArea(this);

		// add to content pane
		content.add(drawArea, BorderLayout.CENTER);

		// create controls to apply colors and call clear feature
		JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

		/**
		 * Action listener is called by the GUI thread.
		 */
		ActionListener actionListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == clearBtn) {
					clearedLocally();
				} else if (e.getSource() == blackBtn) {
					drawArea.setColor(Color.black);
				} else if (e.getSource() == redBtn) {
					drawArea.setColor(Color.red);
				} else if (e.getSource() == boardComboBox) {
					if(modifyingComboBox) return;
					if(boardComboBox.getSelectedIndex()==-1) return;
					String selectedBoardName=(String) boardComboBox.getSelectedItem();
					if(whiteboards.get(selectedBoardName)==null) {
						log.severe("selected a board that does not exist: "+selectedBoardName);
						return;
					}
					selectedBoard = whiteboards.get(selectedBoardName);
					// remote boards can't have their shared status modified
					if(selectedBoard.isRemote()) {
						sharedCheckbox.setEnabled(false);
						sharedCheckbox.setVisible(false);
					} else {
						modifyingCheckBox=true;
						sharedCheckbox.setSelected(selectedBoard.isShared());
						modifyingCheckBox=false;
						sharedCheckbox.setEnabled(true);
						sharedCheckbox.setVisible(true);
					}
					selectedABoard();
				} else if (e.getSource() == createBoardBtn) {
					createBoard();
				} else if (e.getSource() == undoBtn) {
					if(selectedBoard==null) {
						log.severe("there is no selected board to undo");
						return;
					}
					undoLocally();
				} else if (e.getSource() == deleteBoardBtn) {
					if(selectedBoard==null) {
						log.severe("there is no selected board to delete");
						return;
					}
					deleteBoard(selectedBoard.getName());
				}
			}
		};

		clearBtn = new JButton("Clear Board");
		clearBtn.addActionListener(actionListener);
		clearBtn.setToolTipText("Clear the current board - clears remote copies as well");
		clearBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		blackBtn = new JButton("Black");
		blackBtn.addActionListener(actionListener);
		blackBtn.setToolTipText("Draw with black pen");
		blackBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		redBtn = new JButton("Red");
		redBtn.addActionListener(actionListener);
		redBtn.setToolTipText("Draw with red pen");
		redBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		deleteBoardBtn = new JButton("Delete Board");
		deleteBoardBtn.addActionListener(actionListener);
		deleteBoardBtn.setToolTipText("Delete the current board - only deletes the board locally");
		deleteBoardBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		createBoardBtn = new JButton("New Board");
		createBoardBtn.addActionListener(actionListener);
		createBoardBtn.setToolTipText("Create a new board - creates it locally and not shared by default");
		createBoardBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		undoBtn = new JButton("Undo");
		undoBtn.addActionListener(actionListener);
		undoBtn.setToolTipText("Remove the last path drawn on the board - triggers an undo on remote copies as well");
		undoBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		sharedCheckbox = new JCheckBox("Shared");
		sharedCheckbox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(!modifyingCheckBox) setShare(e.getStateChange()==1);
			}
		});
		sharedCheckbox.setToolTipText("Toggle whether the board is shared or not - tells the whiteboard server");
		sharedCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);


		// create a drop list for boards to select from
		JPanel controlsNorth = new JPanel();
		boardComboBox = new JComboBox<String>();
		boardComboBox.addActionListener(actionListener);


		// add to panel
		controlsNorth.add(boardComboBox);
		controls.add(sharedCheckbox);
		controls.add(createBoardBtn);
		controls.add(deleteBoardBtn);
		controls.add(blackBtn);
		controls.add(redBtn);
		controls.add(undoBtn);
		controls.add(clearBtn);

		// add to content pane
		content.add(controls, BorderLayout.WEST);
		content.add(controlsNorth,BorderLayout.NORTH);

		frame.setSize(600, 600);

		// create an initial board
		createBoard();

		// closing the application
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent windowEvent) {
				if (JOptionPane.showConfirmDialog(frame,
						"Are you sure you want to close this window?", "Close Window?",
						JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
				{
					guiShutdown();
					frame.dispose();
				}
			}
		});

		// show the swing paint result
		frame.setVisible(true);

	}

	/**
	 * Update the GUI's list of boards. Note that this method needs to update data
	 * that the GUI is using, which should only be done on the GUI's thread, which
	 * is why invoke later is used.
	 *
	 * @param select, board to select when list is modified or null for default
	 *                selection
	 */
	private void updateComboBox(String select) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				modifyingComboBox=true;
				boardComboBox.removeAllItems();
				int anIndex=-1;
				synchronized(whiteboards) {
					ArrayList<String> boards = new ArrayList<String>(whiteboards.keySet());
					Collections.sort(boards);
					for(int i=0;i<boards.size();i++) {
						String boardname=boards.get(i);
						boardComboBox.addItem(boardname);
						if(select!=null && select.equals(boardname)) {
							anIndex=i;
						} else if(anIndex==-1 && selectedBoard!=null &&
								selectedBoard.getName().equals(boardname)) {
							anIndex=i;
						}
					}
				}
				modifyingComboBox=false;
				if(anIndex!=-1) {
					boardComboBox.setSelectedIndex(anIndex);
				} else {
					if(whiteboards.size()>0) {
						boardComboBox.setSelectedIndex(0);
					} else {
						drawArea.clear();
						createBoard();
					}
				}

			}
		});
	}

}
