/*******************************************************************************
* Copyright 2018 Infostretch Corporation
*
* This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
*
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
* OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
* OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE
*
* You should have received a copy of the GNU General Public License along with this program in the name of LICENSE.txt in the root folder of the distribution. If not, see https://opensource.org/licenses/gpl-3.0.html
*
*
* For any inquiry or need additional information, please contact qmetrysupport@infostretch.com
*******************************************************************************/
package com.qmetry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;
import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import javax.xml.bind.DatatypeConverter;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.Secret;

@IgnoreJRERequirement
public class UploadToServerV4 {

	public Map<String, String> uploadToTheServer(String jiraUrlServer, String username_chkd, Secret password_chkd,
			String apikey, String file, boolean attachFile, String format, String testCycleToReuse, String environment,
			String build, String testCycleLabels, String testCycleComponents, String testCyclePriority,
			String testCycleStatus, String testCycleSprintId, String testCycleFixVersionId, String testCycleSummary,
			String testCaseLabels, String testCaseComponents, String testCasePriority, String testCaseStatus,
			String testCaseSprintId, String testCaseFixVersionId, int buildnumber, Run<?, ?> run, TaskListener listener,
			FilePath workspace, String pluginName)
			throws MalformedURLException, IOException, UnsupportedEncodingException, ProtocolException, ParseException,
			FileNotFoundException, InterruptedException {

		PrintStream logger = listener.getLogger();

		File resultFile = FindFile.findFile(file, run, listener, format, workspace);
		if (resultFile == null) {
			return null;
		}

		Map<String, String> map = new HashMap<String, String>();

		if (jiraUrlServer != null && jiraUrlServer.length() > 0
				&& jiraUrlServer.charAt(jiraUrlServer.length() - 1) == '/') {
			jiraUrlServer = jiraUrlServer.substring(0, jiraUrlServer.length() - 1);
		}

		String uploadserverurlv4 = jiraUrlServer + "/rest/qtm4j/automation/latest/importresult";
		String toEncode = username_chkd.trim() + ":" + password_chkd.getPlainText().trim();

		byte[] mes = toEncode.getBytes("UTF-8");
		String encodedString = DatatypeConverter.printBase64Binary(mes);
		String basicAuth = "Basic " + encodedString;

		String encoding = "UTF-8";
		URL url = new URL(uploadserverurlv4);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		connection.setRequestProperty("apiKey", apikey.trim());
		connection.setRequestProperty("Authorization", basicAuth);
		connection.setDoInput(true);
		connection.setDoOutput(true);

		/// Add proxy URL
		String filepath = "";
		boolean iszip = false;
		if (resultFile.isDirectory()) {
			iszip = true;
			logger.println(pluginName + "Given Path is Directory.");
			logger.println(pluginName + "Creating Zip...");
			filepath = CreateZip.createZip(resultFile.getAbsolutePath(), format, attachFile);
			logger.println(pluginName + "Zip file path : " + filepath);
		} else {
			filepath = resultFile.getAbsolutePath();
			// for zip
			String fileExtension = "";
			if (filepath.contains(".") && filepath.lastIndexOf(".") != 0) {
				fileExtension = filepath.substring(filepath.lastIndexOf(".") + 1);
			}
			if (fileExtension.equals("zip")) {
				iszip = true;
			}
		}

		Map<String, Object> requestDataMap = new HashMap<>();

		String format_short = "";
		if (String.valueOf(format).equals("testng/xml")) {
			format_short = "testng";
		} else if (String.valueOf(format).equals("junit/xml")) {
			format_short = "junit";
		} else if (String.valueOf(format).equals("qas/json")) {
			format_short = "qaf";
		} else if (String.valueOf(format).equals("cucumber/json")) {
			format_short = "cucumber";
		} else if (String.valueOf(format).equals("hpuft/xml")) {
			format_short = "hpuft";
		} else if (String.valueOf(format).equals("specflow/json")) {
			format_short = "specflow";
		}
		requestDataMap.put("format", String.valueOf(format_short));
		requestDataMap.put("isZip", String.valueOf(iszip));
		if (attachFile) {
			requestDataMap.put("attachFile", String.valueOf(attachFile));
		}
		if (testCycleToReuse != null && !testCycleToReuse.isEmpty()) {
			requestDataMap.put("testCycleToReuse", testCycleToReuse.trim());
		}

		if (environment != null && !environment.isEmpty()) {
			requestDataMap.put("environment", environment.trim());
		}

		if (build != null && !build.isEmpty()) {
			requestDataMap.put("build", build.trim());
		}

		Map<String, Object> testcycleDataMap = new HashMap<>();
		boolean isTestcycle = false;
		if (testCycleLabels != null && !testCycleLabels.isEmpty()) {
			isTestcycle = true;
			JSONArray mJSONArray = commaSepratedStringtoJson(testCycleLabels);
			testcycleDataMap.put("labels", mJSONArray);
		}
		if (testCycleComponents != null && !testCycleComponents.isEmpty()) {
			isTestcycle = true;
			JSONArray mJSONArray = commaSepratedStringtoJson(testCycleComponents);
			testcycleDataMap.put("components", mJSONArray);
		}
		if (testCyclePriority != null && !testCyclePriority.isEmpty()) {
			isTestcycle = true;
			testcycleDataMap.put("priority", testCyclePriority.trim());
		}
		if (testCycleStatus != null && !testCycleStatus.isEmpty()) {
			isTestcycle = true;
			testcycleDataMap.put("status", testCycleStatus.trim());
		}
		if (testCycleSprintId != null && !testCycleSprintId.isEmpty()) {
			isTestcycle = true;
			testcycleDataMap.put("sprintId", testCycleSprintId.trim());
		}
		if (testCycleFixVersionId != null && !testCycleFixVersionId.isEmpty()) {
			isTestcycle = true;
			testcycleDataMap.put("fixVersionId", testCycleFixVersionId.trim());
		}
		if (testCycleSummary != null && !testCycleSummary.isEmpty()) {
			isTestcycle = true;
			testcycleDataMap.put("summary", testCycleSummary.trim() + "_" + buildnumber);
		}

		Map<String, Object> testcaseDataMap = new HashMap<>();
		boolean isTestcase = false;
		if (testCaseLabels != null && !testCaseLabels.isEmpty()) {
			isTestcase = true;
			JSONArray mJSONArray = commaSepratedStringtoJson(testCaseLabels);
			testcaseDataMap.put("labels", mJSONArray);
		}
		if (testCaseComponents != null && !testCaseComponents.isEmpty()) {
			isTestcase = true;
			JSONArray mJSONArray = commaSepratedStringtoJson(testCaseComponents);
			testcaseDataMap.put("components", mJSONArray);
		}
		if (testCasePriority != null && !testCasePriority.isEmpty()) {
			isTestcase = true;
			testcaseDataMap.put("priority", testCasePriority.trim());
		}
		if (testCaseStatus != null && !testCaseStatus.isEmpty()) {
			isTestcase = true;
			testcaseDataMap.put("status", testCaseStatus.trim());
		}
		if (testCaseSprintId != null && !testCaseSprintId.isEmpty()) {
			isTestcase = true;
			testcaseDataMap.put("sprintId", testCaseSprintId.trim());
		}
		if (testCaseFixVersionId != null && !testCaseFixVersionId.isEmpty()) {
			isTestcase = true;
			testcaseDataMap.put("fixVersionId", testCaseFixVersionId.trim());
		}

		Map<String, Object> testCaseCycleDataMap = new HashMap<>();
		if (isTestcycle) {
			testCaseCycleDataMap.put("testCycle", testcycleDataMap);
			logger.println(pluginName + "TestCycle  : " + testcycleDataMap);
		}
		if (isTestcase) {
			testCaseCycleDataMap.put("testCase", testcaseDataMap);
			logger.println(pluginName + "TestCase : " + testcaseDataMap);
		}

		requestDataMap.put("fields", testCaseCycleDataMap);

		JSONObject jsonbody = new JSONObject(requestDataMap);
		logger.println(pluginName + "JsonBody : " + jsonbody);

		OutputStream os = connection.getOutputStream();
		os.write(jsonbody.toString().getBytes("UTF-8"));
		// Get the response code
		int statusCode = connection.getResponseCode();
		logger.println(pluginName + "Generate URL API response code : " + statusCode);
		InputStream fis = null;
		if (statusCode >= 200 && statusCode < 400) {
			fis = connection.getInputStream();
		} else {
			fis = connection.getErrorStream();
		}

		StringWriter response = new StringWriter();
		IOUtils.copy(fis, response, encoding);

		JSONParser parser = new JSONParser();
		logger.println(pluginName + "Generate URL API response : " + response.toString());

		JSONObject obj = (JSONObject) parser.parse(response.toString());
		if (obj != null) {
			String resUrl = (String) obj.get("url");
			if (resUrl != null && !resUrl.isEmpty()) {
				// Call another method to upload to S3 bucket.
				String res = uploadToS3(response.toString(), filepath, apikey, basicAuth, logger, pluginName, jiraUrlServer);
				map.put("success", "true");
				map.put("message", res);
			} else {
				map.put("success", "false");
				map.put("errorMessage", response.toString());
			}
		}
		return map;
	}

	public String uploadToS3(String response, String fileurl, String apiKey, String basicAuth, PrintStream logger,
			String pluginName, String jiraUrlServer) throws IOException, org.json.simple.parser.ParseException {

		JSONParser parser = new JSONParser();
		Object obj = parser.parse(response);
		JSONObject jsonObject = (JSONObject) obj;
		String urlForUpload = (String) jsonObject.get("url");
		String trackingId = (String) jsonObject.get("trackingId");
		String uploadResponseString = "";

		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {
			HttpPost httppost = new HttpPost(urlForUpload);
			httppost.addHeader("apikey", apiKey);
			httppost.addHeader("Authorization", basicAuth);
			FileBody fileBody = new FileBody(new File(fileurl));

			HttpEntity reqEntity = MultipartEntityBuilder.create().addPart("file", fileBody).build();
			httppost.setEntity(reqEntity);

			CloseableHttpResponse uploadResponse = httpclient.execute(httppost);
			try {
				int statusCode = uploadResponse.getStatusLine().getStatusCode();
				logger.println(pluginName + "File upload status code : " + uploadResponse.getStatusLine().getStatusCode());
				if (statusCode == 204) {
					logger.println(pluginName + "File uploaded successfully.");
					uploadResponseString = checkStatus(jiraUrlServer, trackingId, apiKey, basicAuth, logger, pluginName);
				} else {
					uploadResponseString = "false";
				}
			} catch (Exception e) {
				logger.println(pluginName + "ERROR - Upload file : " + e.getMessage());
			} finally {
				uploadResponse.close();
			}
		} catch (Exception e) {
			logger.println(pluginName + "ERROR - Upload file : " + e.getMessage());
		} finally {
			httpclient.close();
		}
		return uploadResponseString;
	}

	public String checkStatus(String jiraUrlServer, String trackingId, String apiKey, String basicAuth, PrintStream logger, String pluginName) {
		
		String trackUrl = jiraUrlServer + "/rest/qtm4j/automation/latest/importresult/track?trackingId=" + trackingId;
		String trackResponseString = "";
		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {
			HttpGet httpGet = new HttpGet(trackUrl);
			httpGet.addHeader("apikey", apiKey);
			httpGet.addHeader("Authorization", basicAuth);
			httpGet.addHeader("Content-Type", "application/json");

			HttpResponse trackResponse = httpclient.execute(httpGet);
			HttpEntity entity = trackResponse.getEntity();
			trackResponseString = EntityUtils.toString(entity, "UTF-8");
			try {
				int statusCode = trackResponse.getStatusLine().getStatusCode();
				logger.println(pluginName + "Progress response code : " + statusCode);	
			} catch (Exception e) {
				trackResponseString = e.getMessage();
			} finally {
				trackResponse.getEntity().getContent().close();
			}
		} catch (Exception e) {
			trackResponseString = e.getMessage();
		}
		return trackResponseString;
	}

	public JSONArray commaSepratedStringtoJson(String commaSeparatedString) {
		String[] arrayStr = commaSeparatedString.split(",");
		JSONArray mJSONArray = new JSONArray();
		for (String s : arrayStr) {
			mJSONArray.put(s.trim());
		}
		return mJSONArray;
	}
}