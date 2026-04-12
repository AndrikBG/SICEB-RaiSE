package com.siceb.platform.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class TenantFilterTest {

    private final TenantFilter filter = new TenantFilter();

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void setsBranchFromHeader() throws Exception {
        UUID branchId = UUID.randomUUID();
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getHeader(TenantFilter.BRANCH_HEADER)).thenReturn(branchId.toString());

        doAnswer(invocation -> {
            assertEquals(branchId, TenantContext.get().orElseThrow());
            return null;
        }).when(chain).doFilter(req, res);

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        assertTrue(TenantContext.get().isEmpty(), "Context must be cleared after filter");
    }

    @Test
    void noHeaderLeavesContextEmpty() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getHeader(TenantFilter.BRANCH_HEADER)).thenReturn(null);

        doAnswer(invocation -> {
            assertTrue(TenantContext.get().isEmpty());
            return null;
        }).when(chain).doFilter(req, res);

        filter.doFilter(req, res, chain);
    }

    @Test
    void clearsContextEvenOnException() throws Exception {
        UUID branchId = UUID.randomUUID();
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getHeader(TenantFilter.BRANCH_HEADER)).thenReturn(branchId.toString());
        doThrow(new RuntimeException("test")).when(chain).doFilter(req, res);

        assertThrows(RuntimeException.class, () -> filter.doFilter(req, res, chain));
        assertTrue(TenantContext.get().isEmpty(), "Context must be cleared even on error");
    }
}
