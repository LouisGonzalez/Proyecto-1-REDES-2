import java.io.*;
import java.util.*;

public class proyecto {

	public static void main(String[] args){
		Operations op = new Operations();
		try{
			op.createProject();
		} catch(IOException ioe){
			System.out.println(ioe);
		}
	}
}

public class Operations{

	public static String networkText = "";
	public static String userText = "";
	public static String protocolText = "";	
	public static String modeText = "";
	
	public String textBash = "";
	public String textBash2 = "";
	public String textProto = "";
	
	public DataUser data = new DataUser();
	private ArrayList<Protocol> listProtocol = new ArrayList<>();
	private ArrayList<BandwithControl> listBdw = new ArrayList<>();
	private ArrayList<String> tmpMAC = new ArrayList<>();

	
	public void initialsConfigurations() throws IOException{
		shellCommands("iptables -P INPUT ACCEPT");
		shellCommands("iptables -P FORWARD ACCEPT");
		shellCommands("iptables -P OUTPUT ACCEPT");
		tcInitial();
	}
	
	public void shellCommands(String command) throws IOException{
		Process cmd = Runtime.getRuntime().exec(command);
		BufferedReader reader = new BufferedReader(new InputStreamReader(cmd.getInputStream()));
		String line = "";
		while((line = reader.readLine()) != null){
			System.out.println(line);
		}
	}
	
	public void createProject() throws IOException{
		try{	
			buildMode();
			buildNetwork();
			buildUser();
			buildProtocol();
			initialsConfigurations();
			if(data.getListMode().get(data.getListMode().size()-1)==1){
				modality(1);
				//protocolOperations();
			} else if(data.getListMode().get(data.getListMode().size()-1)==0){
				modality(0);
				//protocolOperations();
			} else {
				System.out.println("Valor de modalidad incorrecto");
			}
		} catch(FileNotFoundException io){
			//error
		} catch(IOException ioe){
			//error
		}
	}
	
	public void tcInitial() throws IOException{
		File shBandwith = new File("/home/luis/Documentos/bandwith1.sh");
		BufferedWriter buffer;
		if(shBandwith.exists()){
			shBandwith.delete();
			tcInitial();
		} else {
			for(int i = 0; i < listBdw.size(); i++){
				boolean exist = false;
				if(!tmpMAC.isEmpty()){
					for(int j = 0; j < tmpMAC.size(); j++){
						if(listBdw.get(i).getMAC().equals(tmpMAC.get(j))){
							exist = true; 							break;				
						}
					}
					if(!exist){
						tmpMAC.add(listBdw.get(i).getMAC());
					}
				} else {
					tmpMAC.add(listBdw.get(i).getMAC());
				}
			}
			for(int i = 0;i < tmpMAC.size();i++){
				int j = i+1;
				textBash += "MAC"+j+"="+tmpMAC.get(i)+"\n";
			}
			textBash += "DEV=enp0s8\n"
			+"TC=$(which tc)\n"
			+"TCF=\"${TC} filter add dev $DEV parent 1: protocol ip prio 5 u32 match u16 0x0800 0xFFFF at -2\"\n\n"
			+"filter_mac() {\n\n"
			+"  M0=$(echo $1 | cut -d : -f 1)$(echo $1 | cut -d : -f 2)\n"
			+"  M1=$(echo $1 | cut -d : -f 3)$(echo $1 | cut -d : -f 4)\n"
			+"  M2=$(echo $1 | cut -d : -f 5)$(echo $1 | cut -d : -f 6)\n\n"
			+"  $TCF match u16 0x${M2} 0xFFFF at -4 match u32 0x${M0}${M1} 0xFFFFFFFF at -8 flowid $2\n"
			+"  $TCF match u32 0x${M1}${M2} 0xFFFFFFFF at -12 match u16 0x${M0} 0xFFFF at -14 flowid $2\n"
			+"  echo \"${M2} ${M1} ${M0}\"\n"
			+"}\n\n"
			+"tc qdisc del dev enp0s8 root\n"
			+"insmod sch_htb 2> /dev/null\n\n"
			+"$TC qdisc add dev $DEV root	  handle 1:    htb default 0xA\n";
			for(int i = 0; i < tmpMAC.size(); i++){
				textBash += "$TC class add dev $DEV parent 1:1 classid 1:2"+i+" htb rate 1kbit\n";
			}	
			for(int i = 0; i < tmpMAC.size(); i++){
				int j = i + 1;
				textBash += "filter_mac $MAC"+j+" 1:2"+i+"\n"; 
			}	
			shBandwith.createNewFile();
			shellCommands("chmod 777 /home/luis/Documentos/	bandwith1.sh");
			writeFile(shBandwith, textBash);
			shellCommands("./bandwith1.sh");
			textBash = "";
		}
	}
	
	
	
	public void modality(int type) throws IOException{
		for(int i = 0; i < listBdw.size(); i++){
			int aux = -1;
			for(int j = 0; j < tmpMAC.size(); j++){
					if(listBdw.get(i).getMAC().equals(tmpMAC.get(j))){
					aux = j;
					break;
				}
			}
			int bdDown = calculateBandwith(listBdw.get(i).getDown(), data.getDown()); 
			int bdUp =  calculateBandwith(listBdw.get(i).getUp(), data.getUp());
			int bdTotal = bdDown + bdUp;
				String[] hora = listBdw.get(i).getScheduleOne().split(":");	
				String[] hora2 = listBdw.get(i).getScheduleTwo().split(":");			
			if(type == 1){
				textBash2 += hora[1]+" "+hora[0]+" * * * /sbin/tc class change dev enp0s8 parent 1:1 classid 1:2"+aux+" htb rate "+bdTotal+"kbit\n";				
				textBash2 += hora2[1]+" "+hora2[0]+" * * * /sbin/tc class change dev enp0s8 parent 1:1 classid 1:2"+aux+" htb rate 1kbit\n";				
			} else if(type == 0){
				int max = data.getDown()*1024;
				textBash2 += hora[1]+" "+hora[0]+" * * * /sbin/tc class change dev enp0s8 parent 1:1 classid 1:2"+aux+" htb rate "+bdTotal+"kbit ceil "+max+"kbit\n";		
				textBash2 += hora2[1]+" "+hora2[0]+" * * * /sbin/tc class change dev enp0s8 parent 1:1 classid 1:2"+aux+" htb rate 1kbit\n";				
			}
		}
		File file = new File("/home/luis/Documentos/crontab/crontab");		
		if(file.exists()){
			file.delete();
			file.createNewFile();
		} else {
			file.createNewFile();
		}
		writeFile(file,textBash2);
		shellCommands("chmod 777 /home/luis/Documentos/crontab/crontab");
		shellCommands("crontab /home/luis/Documentos/crontab/crontab");
		shellCommands("sudo crontab -u root /home/luis/Documentos/crontab/crontab");
		textBash2 = "";
	
	}
	
	
	
	public void protocolOperations() throws IOException{
		File ip = new File("/home/luis/Documentos/iptables.sh");
		if(ip.exists()){
			ip.delete();
			protocolOperations();
		} else {
			for(int i = 0; i < listProtocol.size(); i++){
					if(listProtocol.get(i).getProtocol().equals("icmp")){
			//FORWARD
					textProto += "iptables -I FORWARD  1 -p "+listProtocol.get(i).getProtocol()+" -m --mac-source "+listProtocol.get(i).getMAC()+" -m time --timestart "+listProtocol.get(i).getScheduleOne()+" --timestop "+listProtocol.get(i).getScheduleTwo()+" -j ACCEPT\n";
					textProto += "iptables -I FORWARD 1 -p icmp -m state --state RELATED,ESTABLISHED -m time --timestart "+listProtocol.get(i).getScheduleOne()+" --timestop "+listProtocol.get(i).getScheduleTwo()+" -j ACCEPT\n";
				} else {
				//FORWARD
					textProto += "iptables -I FORWARD 1 -p "+listProtocol.get(i).getProtocol()+" -m --mac-source "+listProtocol.get(i).getMAC()+" -m "+listProtocol.get(i).getProtocol()+" --dport "+listProtocol.get(i).getPort()+" -m time --timestart "+listProtocol.get(i).getScheduleOne()+" --timestop "+listProtocol.get(i).getScheduleTwo()+" -j ACCEPT\n";
					textProto += "iptables -I FORWARD 1 -p "+listProtocol.get(i).getProtocol()+" -m state --state RELATED,ESTABLISHED -m "+listProtocol.get(i).getProtocol()+" --sport "+listProtocol.get(i).getPort()+" -m time --timestart "+listProtocol.get(i).getScheduleOne()+" --timestop "+listProtocol.get(i).getScheduleTwo()+" -j ACCEPT\n";	
				}
			}
			ip.createNewFile();
			shellCommands("chmod 777 /home/luis/Documentos/iptables.sh");
			writeFile(ip, textProto);
			shellCommands("./iptables.sh");
			textProto = "";
		}
	}
	
	
	public void buildMode()throws FileNotFoundException, IOException {
		almacenateConf(1);
		String[] lines = modeText.split("\n");
		for(int i = 0; i < lines.length; i++){
			String[] dataLine = lines[i].split("=");
			int temp = Integer.parseInt(dataLine[1]);
			data.getListMode().add(temp);
		}
	}

	public void buildNetwork()throws FileNotFoundException, IOException {
		almacenateConf(2);
		String[] lines = networkText.split("\n");
		for(int i = 0; i < lines.length; i++){
			String[] dataLine = lines[i].split("=");
			int velocity = Integer.parseInt(dataLine[1]);
			if(dataLine[0].equals("down")){
				data.setDown(velocity);
				System.out.println(data.getDown()+"down");
			} else if(dataLine[0].equals("up")){
				data.setUp(velocity);
				System.out.println(data.getUp()+"up" );
			} else {
				System.out.println("no se reconoce el id dentro del archivo enlace.conf");
			}
		}
	}

	public void buildUser()throws FileNotFoundException, IOException {
		almacenateConf(3);
		String[] lines = userText.split("\n");
		for(int i = 0; i < lines.length; i++){
			String[] dataLine = lines[i].split(",");
			BandwithControl bd = new BandwithControl();
			bd.setMAC(dataLine[0]);
			bd.setDown(Integer.parseInt(dataLine[1]));
			bd.setUp(Integer.parseInt(dataLine[2]));
			bd.setScheduleOne(dataLine[3]);
			bd.setScheduleTwo(dataLine[4]);
			listBdw.add(bd);	
		}
	}

	public void buildProtocol()throws FileNotFoundException, IOException {
		almacenateConf(4);
		String[] lines = protocolText.split("\n");
		for(int i = 0; i < lines.length; i++){
			String[] dataLine = lines[i].split(",");
			Protocol pt = new Protocol();
			pt.setMAC(dataLine[0]);
			pt.setProtocol(dataLine[1]);
			pt.setPort(dataLine[2]);
			pt.setScheduleOne(dataLine[3]);
			pt.setScheduleTwo(dataLine[4]);
			listProtocol.add(pt);
		}
	}

	
	public void almacenateConf(int conf) throws FileNotFoundException, IOException {
		File archivo;
		FileReader reader;
		BufferedReader buffer;
		String temporal = "";
		switch(conf){
		case 1:
			//modo.conf
			reader = new FileReader("modo.conf");
			buffer = new BufferedReader(reader);
			while((temporal = buffer.readLine())!= null){
				modeText += temporal+"\n";
			}
			buffer.close();
			break;
		case 2:
			//enlace.conf
			reader = new FileReader("enlace.conf");
			buffer = new BufferedReader(reader);
			while((temporal = buffer.readLine())!= null){
				networkText += temporal + "\n";  
			}
			buffer.close();
			break;
		case 3:
			//usuarioBW.conf
			reader = new FileReader("usuario_BW.conf");
			buffer = new BufferedReader(reader);
			while((temporal = buffer.readLine())!= null){
				userText += temporal + "\n";
			}
			buffer.close();
			break;
		case 4:
			//protocolo.conf
			reader = new FileReader("usuario_Proto.conf");
			buffer = new BufferedReader(reader);
			while((temporal = buffer.readLine())!= null){
				protocolText += temporal + "\n"; 
			}
			buffer.close();
			break;
		default:
		
			break;
		}
	}

	public int calculateBandwith(int percentage, int totalBd){
		int total = 0;
		totalBd = totalBd * 1024;
		total = (totalBd * percentage) / 100;
		return total;
	}
	
	public void writeFile(File file, String text){
		FileWriter fw = null;
		PrintWriter pw = null;
		try{
			fw = new FileWriter(file);
			pw = new PrintWriter(fw);
			pw.print(text);
			fw.close();
		} catch(Exception e){
			System.out.println(e);
		}
		
	}

}	

public class DataUser {
	
	//-------
	private ArrayList<Integer> listMode = new ArrayList<>();
	//-------
	private ArrayList<String> listProtocol = new ArrayList<>();
	private ArrayList<String> listPorts = new ArrayList<>();
	//-------
	private Integer up;
	private Integer down;
	
	public Integer getUp(){
		return up;
	}

	public void setUp(Integer up){
		this.up = up;
	}

	public Integer getDown(){
		return down;
	}
	
	public void setDown(Integer down){
		this.down = down;
	}

	public ArrayList<String> getListPorts(){
		return listPorts;
	}
	
	public void setListPorts(ArrayList<String> listPorts){
		this.listPorts = listPorts;
	}
	
	public ArrayList<Integer> getListMode(){
		return listMode;
	}
	
	public void setListMode(ArrayList<Integer> listMode){
		this.listMode = listMode;
	}

	public ArrayList<String> getListProtocol(){
		return listProtocol;
	}
	
	public void setListProtocol(ArrayList<String> listProtocol){
		this.listProtocol = listProtocol;
	}

}	

public class BandwithControl {

	private String MAC;
	private Integer down;
	private Integer up;
	private String scheduleOne;
	private String scheduleTwo;


	public String getMAC(){
		return MAC;
	}
	
	public void setMAC(String MAC){
		this.MAC = MAC;
	}
	
		public Integer getDown(){
		return down;
	}
	
	public void setDown(Integer down){
		this.down = down;
	}
	
	public Integer getUp(){
		return up;
	}
	
	public void setUp(Integer up){
		this.up = up;
	}

	
	public String getScheduleOne(){
		return scheduleOne;
	}
	
	public void setScheduleOne(String scheduleOne){
		this.scheduleOne = scheduleOne;
	}
	
	public String getScheduleTwo(){
		return scheduleTwo;
	}
	
	public void setScheduleTwo(String scheduleTwo){
		this.scheduleTwo = scheduleTwo;
	}

}

public class Protocol {

	private String MAC;
	private String protocol;
	private String port;
	private String scheduleOne;
	private String scheduleTwo;
	
	public String getMAC(){
		return MAC;
	}
	
	public void setMAC(String MAC){
		this.MAC = MAC;
	}
	
	public String getProtocol(){
		return protocol;
	}
	
	public void setProtocol(String protocol){
		this.protocol = protocol;
	}
	
	public String getPort(){
		return protocol;
	}
	
	public void setPort(String port){
		this.port = port;
	}	
	
	public String getScheduleOne(){
		return scheduleOne;
	}
	
	public void setScheduleOne(String scheduleOne){
		this.scheduleOne = scheduleOne;
	}
	
	public String getScheduleTwo(){
		return scheduleTwo;
	}
	
	public void setScheduleTwo(String scheduleTwo){
		this.scheduleTwo = scheduleTwo;
	}
}
	
	
