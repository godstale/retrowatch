package com.hardcopy.retrowatchle.connectivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import android.util.Log;

/*
	만든이 : 고하나 (고또)
	만든 날짜 : 2011.02.26
	플랫폼 : 윈도 7 64bit, 이클립스, 안드로이드 SDK(2.1 ver7)
*/


/// HTTP방식으로 데이터를 주고 리퀘스트 받아오는 클래스
/// GET와 POST방식 두가지 다 사용 가능하다.

/// 생성자 호출해서 클래스 인스턴스 생성한 다음
/// request ( URL주소, 방식(GET or POST), 변수명+변수값 ) ;  함수로  보내고 받을 수 있다.

public class HttpRequester 
{
	public String m_request ;						/// 리퀘스트 내용을 통채로 저장할 스트링
	private HttpURLConnection m_con ;		/// http방식으로 연결을 유지할 커넥션
	String m_cookies = "" ;							/// 세션 유지에 필요한 쿠키
	boolean m_session = false ;					/// 로그인 해서 세션 가지고 있는지 여부
	long m_sessionLimitTime = 600000 ; 		/// 세션 시간제한 (밀리세컨드)
	long m_sessionTime = 0 ;						/// 세션을 얻은 시간
	
	private static final String ENCODING_TYPE_UTF_8 = "UTF-8";
	private static final String ENCODING_TYPE_EUC_KR = "EUC-KR";
	private static int TIMEOUT_VALUE = 5000;

	HttpRequester( )				/// 생성자
	{}
	
	
	/// 1. 세션이 유지되고있는지 체크
	/// 2. 시간을 넘겼어도 세션 제거하고 false~
	public boolean checkSession( ) 
	{
		if( !m_session ) {
			return false ;
		}
		
		if( System.currentTimeMillis( ) < m_sessionTime + m_sessionLimitTime ) {
			m_sessionTime = System.currentTimeMillis( ) ;		/// 제한시간 아직 안넘었음 세션 유지 연장시킴
			return true ;	
		} else {
			m_session = false ;			/// 제한시간을 넘겼음 세션을 제거함
			return false ;	
		}
	}
	
	/// Request를 받되 세션 유지를 위해 쿠키를 저장한다.
	public String requestAndSetSession( String uri, Map<String, Object> params ) throws MalformedURLException, IOException 	{
		
		String rec = request( new URL(uri), null, "POST", params ) ;				/// 일단 주소에 데이터랑 보내고

    	Map<String, List<String>> imap = m_con.getHeaderFields( ) ;	/// 맵에다 Http헤더를 받아냄
    	if( imap.containsKey( "Set-Cookie" ) )						    	/// 그리고 거길 뒤져서 쿠키를 찾아냄
    	{
			List<String> lString = imap.get( "Set-Cookie" ) ;    		/// 쿠키를 스트링으로 쫙 저장함
			for( int i = 0 ; i < lString.size() ; i++ ) {
				m_cookies += lString.get( i ) ;
			}
        	m_session = true ;    		/// 세션을 저장했으니
        	
    	} else {
    		m_session = false ;
    	}
    	/// 돌려주는 값은 리퀘스트값 뿐이지만  m_session값을 셋팅하니까
    	/// checkSession( ) 함수를 호출해서 쿠키를 성공적으로 얻었는지 확인 가능함
		return rec ;
	}
	
	
	/// 리퀘스트 받아오는 함수
	/// ( URL주소, 방식(GET or POST), 변수명+변수값 ) ;
	protected String request( URL url, String encType, String method, Map<String, Object> params) throws IOException 
	{
		if(url == null) return "";
		
		InputStream in = null ;			/// 받아올 인풋스트림
		OutputStream out = null ;		/// POST방식일 경우 데이터를 전송할 아웃풋 스트림
	
		/// 연결하고 메소드 셋팅함
		m_con = (HttpURLConnection) url.openConnection( ) ;
		///String wwwstring = URLEncoder.encode( url.toString() ) ;
		m_con.setRequestMethod(method);
		m_con.setConnectTimeout(TIMEOUT_VALUE);
		m_con.setReadTimeout(TIMEOUT_VALUE);
	
		/// 인코딩 정의 HTTP방식으로 전송할때는 urlencoded방식으로 인코딩해서 전송해야한다.
		m_con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		
		/// 인풋스트림 쓸거라고 지정
		m_con.setDoInput(true);
		
		if( m_session ) {
			m_con.setRequestProperty( "cookie", m_cookies ) ;
		}
		
		/// 포스트방식일 경우 변수를 outputStream생성해서 서버로 전송
		if (method.equals("POST")) 
		{
			/// 데이터를 주소와 별개로 전송한다.
			m_con.setDoOutput(true);							/// 아웃풋 스트림 쓰기위에 아웃풋을 true로 켬
			
			String paramstr = buildParameters( params ) ;	/// 파라메터를 문자열로 치환
			
			out = m_con.getOutputStream( ) ;				/// 아웃풋 스트림 생성
			out.write( paramstr.getBytes( "UTF-8" ) ) ;		/// UTF-8포멧으로 변경해서 변수를 쓴다.
			out.flush( ) ;									/// 플러쉬~
			out.close( ) ;									/// 스트림 닫기
			// Log.d( "jsonPrint", "post succes" ) ;			/// 로그출력
		}
		
		// SuhYB. Find encoding type to prevent broken 2-byte character
		String encodingType = ENCODING_TYPE_EUC_KR;
		try {
			String headerType = m_con.getContentType();
			if(encType!=null && encType.length()>0)
				encodingType = encType;
			if(headerType !=null && headerType.toUpperCase().indexOf(ENCODING_TYPE_UTF_8) != -1) {
				encodingType = ENCODING_TYPE_UTF_8;
			}
		} catch(Exception e) {
			e.printStackTrace();
			encodingType = ENCODING_TYPE_UTF_8;
		}
	
		/// 받아온 데이터를 씍위한 스트림
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		/// 리퀘스트 데이터를 저장할 버퍼
		byte[] buf = new byte[131072];
		try 
		{
			in = m_con.getInputStream();						/// 인풋스트림 생성
			//Log.d( "---recTime---", "" + (System.currentTimeMillis( ) - ti) ) ; /// == 시간 체크용 == inputstream얻는 요기서 시간 10초이상 넘어가면 큰일남
																				/// 갤럭시 S에서 어떤앱은 WebView라던가 Http통신에서 15초인가 넘어가면 세션 끊기는
																				/// 원인을 알 수 없는 경우도 있었음 다른기기 다 잘되는데 오로지 갤럭시 S만!!! 그랬음 참고 바람요

			/// 루프를 돌면서 리퀘스트로 받은내용을 저장한다.
			while (true) 
			{
				int readlen = in.read(buf);
				if (readlen < 1)
					break;
				bos.write(buf, 0, readlen);
			}
			
			m_request = new String( bos.toByteArray( ), encodingType ) ;				// SuhYB. 특정 페이지들의 글자 깨짐 방지를 위해 지정한 인코딩 타입으로 수신
			/////// 리퀘스트 받은 내용을 UTF-8로 변경해서 문자열로 저장 /////////////////
			/*
			File fl = new File( "/sdcard/rec.txt" ) ;
			FileOutputStream fos = new FileOutputStream( fl ) ;
			fos.write( bos.toByteArray( ) ) ;
			/**/
			
			return m_request ;
		} 
		catch (IOException e) 
		{
			/// 리퀘스트 받다가 에러가 나면 에러나면서 받은 메세지를 읽는다.
			if(m_con != null && m_con.getResponseCode() == 500) 
			{
				/// 버퍼 리셋하고 에러값 받을 인풋스트림 생성해서 레어메세지 얻기
				bos.reset();
				InputStream err = m_con.getErrorStream();
				while (true) 
				{
					int readlen = err.read( buf ) ;
					if ( readlen < 1 )
						break ;
					bos.write( buf, 0, readlen ) ;
				}
				
				/// 에러메세지를 문자열로 저장
				String output = new String(bos.toByteArray(), "UTF-8");
				
				/// 읽은 에러메세지를 출력한다.
				System.err.println(output);
			}
			throw e;
		} 
		finally /// 500에러도 아니면 그냥 접속 끊어버림.... -_- 안되는데 답있나?
		{
			if ( in != null )
				in.close( ) ;
			if ( m_con != null )
				m_con.disconnect( ) ;
		}
	}
	
	/// 파라메터 받은 값을  "변수명=변수값&" 형식의 텍스트로 변환해주는 함수
	protected String buildParameters(Map<String, Object> params) throws IOException 
	{
		if( params == null )
			return "" ;
		
		StringBuilder sb = new StringBuilder( ) ;
		
		/// 잘 아시겠지만 arg1=초코릿&arg2=아이스크림&arg3=핫초코  이런식으로 만들어서 날린다.
		for( Iterator<String> i = params.keySet( ).iterator( )  ; i.hasNext( )  ;  ) 
		{
			String key = (String) i.next();
			sb.append(key);
			sb.append("=");
			sb.append(URLEncoder.encode(String.valueOf(params.get(key)), "UTF-8"));
			if (i.hasNext())
				sb.append("&");
		}
		return sb.toString();
	}
	
}
