package com.difegue.doujinsoft;

import com.difegue.doujinsoft.utils.MioUtils.Types;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// MusicServlet – constructor + doGet + doPost paths.
class TestMusicServlet {

    @Test
    void ctor_isReachable() {
        // constructor line executed
        MusicServlet servlet = new MusicServlet();
        assert servlet != null;
    }

    @Test
    void doGet_delegatesToSuper() throws Exception {
        MusicServlet servlet = Mockito.spy(new MusicServlet());
        //just confirm delegation
        doNothing().when(servlet)
                .doGet(any(HttpServletRequest.class),
                        any(HttpServletResponse.class),
                        eq(Types.RECORD));
        HttpServletRequest  req  = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        servlet.doGet(req, resp);
        // verify the exact super-call MusicServlet
        verify(servlet).doGet(req, resp, Types.RECORD);
    }

    @Test
    void doPost_delegatesToSuper() throws Exception {
        MusicServlet servlet = Mockito.spy(new MusicServlet());
        doNothing().when(servlet)
                .doPost(any(HttpServletRequest.class),
                        any(HttpServletResponse.class),
                        eq(Types.RECORD));
        HttpServletRequest  req  = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        servlet.doPost(req, resp);
        verify(servlet).doPost(req, resp, Types.RECORD);
    }
}
