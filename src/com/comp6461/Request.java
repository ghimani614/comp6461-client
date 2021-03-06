package com.comp6461;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Request {

	private boolean verbose = false;
	private boolean header = false;
	private boolean inlineDataFlag = false;
	private boolean writeToFile = false;
	private boolean fileFlag = false;
	private String method = "";
	private String url = "";
	private List<String> headerValue = new ArrayList<>();
	private String inlineDataValue = "";
	private String fileName = "";
	private String data = "";
	private String outputFileName = "";
	private String redirectUrl = "";

	public String getRedirectUrl() {
		return redirectUrl;
	}

	public void setRedirectUrl(String redirectUrl) {
		this.redirectUrl = redirectUrl;
	}

	public boolean isWriteToFile() {
		return writeToFile;
	}

	public void setWriteToFile(boolean writeToFile) {
		this.writeToFile = writeToFile;
	}

	public String getOutputFileName() {
		return outputFileName;
	}

	public void setOutputFileName(String outputFileName) {
		this.outputFileName = outputFileName;
	}

	Request() {
	}

	public String getMethod() {
		return method;
	}

	void setMethod(String method) {
		this.method = method;
	}

	private boolean isVerbose() {
		return verbose;
	}

	void setVerbose() {
		this.verbose = true;
	}

	private boolean isHeader() {
		return header;
	}

	void setHeader() {
		this.header = true;
	}

	public String getUrl() {
		return url;
	}

	void setUrl(String url) {
		this.url = url;
	}

	private List<String> getHeaderValue() {
		return headerValue;
	}

	void setHeaderValue(List<String> headerValue) {
		this.headerValue = headerValue;
	}

	public boolean isInlineDataFlag() {
		return inlineDataFlag;
	}

	void setInlineDataFlag() {
		this.inlineDataFlag = true;
	}

	public String getInlineDataValue() {
		return inlineDataValue;
	}

	void setInlineDataValue(String inlineDataValue) {
		this.inlineDataValue = inlineDataValue;
	}

	public boolean isFileFlag() {
		return fileFlag;
	}

	void setFileFlag() {
		this.fileFlag = true;
	}

	public String getFileName() {
		return fileName;
	}

	void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	void executeRequest(String receivedUrl) {
		try {
			receivedUrl = this.getUpdatedUrl(receivedUrl);
			String contentPost = "";
			boolean printFromStart = this.isVerbose();
			URL url = new URL(receivedUrl);
			String host = url.getHost();
			String queryString = this.getQueryString(url);
			InetAddress inetAddress = InetAddress.getByName(host);
			Socket socket = new Socket(inetAddress, 80);
			BufferedWriter bufferedWriter = new BufferedWriter(
					new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
			bufferedWriter.write(this.getMethod().toUpperCase() + " " + queryString + " HTTP/1.0\r\n");
			bufferedWriter.write("Host: " + host + "\r\n");
			if (this.getMethod().equalsIgnoreCase("post")) {
				contentPost = this.getDataBasedOnFlagSet();
				bufferedWriter.write("Content-Length: " + contentPost.length() + "\r\n");
			}
			if (this.isHeader() && !this.getHeaderValue().isEmpty()) {
				this.getHeaderValue().forEach(s -> {
					try {
						bufferedWriter.write(s + "\r\n");
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			}
			if (this.getMethod().equalsIgnoreCase("post")) {
				bufferedWriter.write("\r\n");
				bufferedWriter.write(contentPost);
			}
			bufferedWriter.write("\r\n");
			bufferedWriter.flush();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			StringBuilder responseData = new StringBuilder();
			String response = "";
			while ((response = bufferedReader.readLine()) != null) {
				responseData.append(response).append("\n");
			}
			if (isNewRedirectedUrlFound(responseData.toString())) {
				executeRequest(this.getRedirectUrl());
			}
			if (this.isWriteToFile()) {
				writeResponseToFile(responseData.toString());
			} else {
				String[] content = responseData.toString().split("\n");
				for (int i = 0; i < content.length; i++) {
					if (printFromStart) {
						System.out.println(content[i]);
					} else {
						printFromStart = content[i].isEmpty();
					}
				}
			}
			bufferedReader.close();
			bufferedWriter.close();
			socket.close();
		} catch (

		IOException e) {
			e.printStackTrace();
		}
	}

	private boolean isNewRedirectedUrlFound(String responseData) {
		String[] responseArr = responseData.split("\n");
		if (responseArr[0].contains(Constants.MOVED_PERMANENT_CODE)) {
			for (int i = 1; i < responseArr.length; i++) {
				if (responseArr[i].startsWith("Location:")) {
					this.setRedirectUrl(responseArr[i].split(":", 2)[1].trim());
					break;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	private void writeResponseToFile(String responseData) {
		String outputFile = this.getOutputFileName();
		System.out.println("Writing response to file " + outputFile);
		File file = new File(outputFile);
		try {
			if (!file.exists()) {
				if (file.getParent() != null)
					file.getParentFile().mkdirs();
				file.createNewFile();
			}
			file.setWritable(true);
			FileWriter fileWriter = new FileWriter(file, true);
			fileWriter.write(responseData);
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getQueryString(URL url) {
		String queryString = "/";
		if (url.getPath() != null && !url.getPath().equalsIgnoreCase(""))
			queryString += url.getPath();
		if (this.getMethod().equalsIgnoreCase("get")) {
			if (url.getQuery() != null && !url.getQuery().equalsIgnoreCase(""))
				queryString += "?" + url.getQuery();
		}
		return queryString;
	}

	private String getDataBasedOnFlagSet() throws IOException {
		String data = "";
		if (this.isInlineDataFlag()) {
			data = this.inlineDataValue;
		} else if (this.isFileFlag()) {
			data = this.readFileData(this.getFileName());
		}
		return data;
	}

	private String readFileData(String filePath) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath));
		String line = "";
		StringBuilder data = new StringBuilder();
		while ((line = bufferedReader.readLine()) != null) {
			data.append(line);
		}
		bufferedReader.close();
		return data.toString();
	}

	private String getUpdatedUrl(String url) {
		if (url.startsWith("http://") || url.startsWith("https://"))
			return url;
		else {
			url = "http://" + url;
			return url;
		}
	}
}
