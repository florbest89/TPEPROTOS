package proxy_server;

public class ProxyStats {

	// # of bytes transferred
	private long bytesTransferred;
	// # of accesses to proxy
	private long accesses;
	// total amount of +OK code
	private long timesok;
	// total amount of -ERR code
	private long originServErr;
	// total amount of -ERR[NOTADMIN] code
	private long errnotadmin;
	// total amount of -ERR[INVALID] code
	private long errinvalid;
	// total amount of -ERR[USERNEEDED] code
	private long errusrnotprov;
	//total amount of -ERR[FAILED] code
	private long errfailed;

	public ProxyStats() {
	}

	public void addBytesTransf(long bytesTransferred) {
		this.bytesTransferred = this.bytesTransferred + bytesTransferred;
	}

	public void addAccess() {
		this.accesses = this.accesses + 1;
	}

	public void addErrCode() {
		System.out.println("agregue un error");
		this.originServErr = this.originServErr + 1;
	}

	public void addOkCode() {
		this.timesok = this.timesok + 1;
	}

	public void addNotAdmin() {
		this.errnotadmin = this.errnotadmin + 1;
	}

	public void addInvalid() {
		this.errinvalid = this.errinvalid + 1;
	}

	public void addUsrNeeded() {
		this.errusrnotprov = this.errusrnotprov + 1;
	}

	public String getStats() {
		return "#Bytes transferred: " + this.bytesTransferred + "\n#Accesses: "
				+ this.accesses + "\n#+OK status code: " + this.timesok
				+ "\n#-ERR status code: " + getTimesErr() + "\r\n";
	}

	public String getHistogram() {
		return "#+OK : " +this.timesok + "\n# -ERR[NOTADMIN] : " + this.errnotadmin
				+ "\n# -ERR[INVALID] : " + this.errinvalid
				+ "\n# -ERR[USRNEEDED] : " + this.errusrnotprov
				+ "\n# -ERR (origin server) : " + getOriginServerErr() 
				+ "\n# -ERR[FAILED] : " + this.errfailed + "\r\n";
	}
	
	public long getTimesErr(){
		return errnotadmin + errinvalid + errusrnotprov + originServErr + errfailed;
	}

	public long getOriginServerErr() {
		System.out.println("cantidad de errores de origin server " + originServErr);
		return this.originServErr;
	}

	public void addFailed() {
		errfailed =+ 1;		
	}

}
