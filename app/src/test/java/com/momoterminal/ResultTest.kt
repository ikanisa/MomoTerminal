package com.momoterminal

import com.momoterminal.util.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Result sealed class.
 */
class ResultTest {
    
    @Test
    fun `Success result contains data`() {
        val result = Result.Success("test data")
        
        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertFalse(result.isLoading)
        assertEquals("test data", result.getOrNull())
    }
    
    @Test
    fun `Error result contains exception`() {
        val exception = IllegalArgumentException("test error")
        val result = Result.Error(exception)
        
        assertFalse(result.isSuccess)
        assertTrue(result.isError)
        assertFalse(result.isLoading)
        assertNull(result.getOrNull())
    }
    
    @Test
    fun `Loading result indicates loading state`() {
        val result = Result.Loading
        
        assertFalse(result.isSuccess)
        assertFalse(result.isError)
        assertTrue(result.isLoading)
    }
    
    @Test
    fun `getOrElse returns data for Success`() {
        val result = Result.Success("test")
        assertEquals("test", result.getOrElse("default"))
    }
    
    @Test
    fun `getOrElse returns default for Error`() {
        val result: Result<String> = Result.Error(Exception())
        assertEquals("default", result.getOrElse("default"))
    }
    
    @Test
    fun `map transforms Success data`() {
        val result = Result.Success(5)
        val mapped = result.map { it * 2 }
        
        assertTrue(mapped is Result.Success)
        assertEquals(10, (mapped as Result.Success).data)
    }
    
    @Test
    fun `map preserves Error`() {
        val result = Result.Error(Exception("error"))
        val mapped = result.map { "transformed" }
        
        assertTrue(mapped is Result.Error)
    }
    
    @Test
    fun `flatMap chains Success results`() {
        val result = Result.Success(5)
        val chained = result.flatMap { Result.Success(it * 2) }
        
        assertTrue(chained is Result.Success)
        assertEquals(10, (chained as Result.Success).data)
    }
    
    @Test
    fun `flatMap preserves Error in chain`() {
        val result = Result.Success(5)
        val chained = result.flatMap { Result.Error(Exception("error")) }
        
        assertTrue(chained is Result.Error)
    }
    
    @Test
    fun `onSuccess executes action for Success`() {
        var executed = false
        val result = Result.Success("test")
        
        result.onSuccess { executed = true }
        
        assertTrue(executed)
    }
    
    @Test
    fun `onSuccess does not execute for Error`() {
        var executed = false
        val result = Result.Error(Exception())
        
        result.onSuccess { executed = true }
        
        assertFalse(executed)
    }
    
    @Test
    fun `onError executes action for Error`() {
        var executed = false
        val result = Result.Error(Exception())
        
        result.onError { executed = true }
        
        assertTrue(executed)
    }
    
    @Test
    fun `runCatching returns Success for successful block`() {
        val result = Result.runCatching { "success" }
        
        assertTrue(result is Result.Success)
        assertEquals("success", (result as Result.Success).data)
    }
    
    @Test
    fun `runCatching returns Error for throwing block`() {
        val result = Result.runCatching { throw IllegalStateException("error") }
        
        assertTrue(result is Result.Error)
    }
}
