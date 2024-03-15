
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;


public class MonitorClient {
	public static void main(String args[]){
		byte[] receiveData = new byte[1024];
		byte[] sendData = new byte[1024];
		Scanner scn= new Scanner(System.in);
		System.out.println("How many Requests to send?");
		int n = scn.nextInt();
		int port = Integer.parseInt(args[1]);
		long [] startTS = new long[n];
		long [] endTS = new long[n];
		try {
			DatagramSocket clientSocket = new DatagramSocket(8000); //This Client will get response back into port 8000.
			InetAddress IPAddress = InetAddress.getByName(args[0]);
			clientSocket.setSoTimeout(1000);

			for (int i = 0; i < n; i++) {
				String msg= "Hello "+i+" ";
				sendData = msg.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
				long start=System.currentTimeMillis();
				clientSocket.send(sendPacket);
				startTS[i]=start; //store when we sent the msg.
				System.out.println("Send msg num- "+i);
				
			
				try {
					DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
		       		clientSocket.receive(receivePacket);
					String response = new String(receivePacket.getData());
					Scanner word= new Scanner(response); //used to separate the number indicator for each msg response.
					word.next();
					long end=System.currentTimeMillis();
					int index = word.nextInt();
					endTS[index] = end; //store the time we get response for this specific msg by the number indicator.
					
				} 
				catch (SocketTimeoutException e) {
			     		 //if we get no response after 1 sec we continue for the next msg. 
		       		}
			}
			clientSocket.setSoTimeout(5000); //change the time out to 5 sec.
			while(true){
				//if we have any responses in the buffer we process them. otherwise after 5 sec we finish to listen to the socket.
				try {
					DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
		       			clientSocket.receive(receivePacket);
					String response = new String(receivePacket.getData());
					Scanner word= new Scanner(response);
					word.next();
					long end=System.currentTimeMillis();
					int index = word.nextInt();
					endTS[index] = end;
				} 
				catch (SocketTimeoutException e) {
			     		break;    
		       		}
			}
			
			ArrayList<Double> sampleRTT = new ArrayList<>();
			ArrayList<Double> estimatedRTT = new ArrayList<>();
			ArrayList<Double> devRTT = new ArrayList<>();
			
			//in order to filter no reply msgs
			for (int j = 0; j < n; j++) {
				if(endTS[j] == 0)
					System.out.println("Request "+j+": no reply");
				else{	
					Double time = (endTS[j] - startTS[j])*1.0;	
					sampleRTT.add(time);
					System.out.println("Request "+j+": RTT = "+time);
				}
			}
			//if we have at least one response we can obtain estimatedRTT and Dev RTT.
			if(sampleRTT.size() > 0){
				estimatedRTT.add(sampleRTT.get(0)); 
				devRTT.add(sampleRTT.get(0)/2);
				for (int j = 1; j < sampleRTT.size(); j++) {
						estimatedRTT.add((Double)( (1.0-0.125)* estimatedRTT.get(j-1) + (0.125)*sampleRTT.get(j) ) );
						devRTT.add((Double)( ((1.0-0.25)*devRTT.get(j-1) ) + (0.25)*Math.abs(sampleRTT.get(j) - estimatedRTT.get(j-1)) ) );  
				}
				
				System.out.println("Estimated RTT");
				System.out.println(estimatedRTT);
				System.out.println("Dev RTT");
				System.out.println(devRTT);
			}
			
			clientSocket.close();
			scn.close();
		}
		catch (SocketException e) {
			System.out.println("problem with the Socket-" +e);
		}
		catch (Exception e) {
			System.out.println("problem- "+ e);
		}
	}

}
