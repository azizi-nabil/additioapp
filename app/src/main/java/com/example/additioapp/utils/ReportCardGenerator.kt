package com.example.additioapp.utils

import android.content.Context
import android.graphics.Color
import com.example.additioapp.data.model.*
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ReportCardGenerator(private val context: Context) {
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    fun generateReportCard(reportCard: StudentReportCard, outputFile: File): Boolean {
        return try {
            val document = PDDocument()
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)
            
            val contentStream = PDPageContentStream(document, page)
            var yPosition = 750f
            
            // Header
            yPosition = drawHeader(contentStream, reportCard, yPosition)
            
            // Student Info
            yPosition = drawStudentInfo(contentStream, reportCard, yPosition)
            
            // Grades Summary
            yPosition = drawGradesSummary(contentStream, reportCard.gradesSummary, yPosition)
            
            // Attendance Summary
            yPosition = drawAttendanceSummary(contentStream, reportCard.attendanceSummary, yPosition)
            
            // Behavior Summary
            yPosition = drawBehaviorSummary(contentStream, reportCard.behaviorSummary, yPosition)
            
            // Footer
            drawFooter(contentStream, reportCard)
            
            contentStream.close()
            document.save(outputFile)
            document.close()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun drawHeader(stream: PDPageContentStream, reportCard: StudentReportCard, startY: Float): Float {
        var y = startY
        
        // Title
        stream.beginText()
        stream.setFont(PDType1Font.HELVETICA_BOLD, 20f)
        stream.newLineAtOffset(50f, y)
        stream.showText("Student Report Card")
        stream.endText()
        
        y -= 30
        
        // Class and Date
        stream.beginText()
        stream.setFont(PDType1Font.HELVETICA, 12f)
        stream.newLineAtOffset(50f, y)
        stream.showText("Class: ${reportCard.classInfo.name} - ${reportCard.classInfo.year}")
        stream.endText()
        
        stream.beginText()
        stream.newLineAtOffset(400f, y)
        stream.showText("Date: ${dateFormat.format(Date(reportCard.generatedDate))}")
        stream.endText()
        
        y -= 20
        drawLine(stream, 50f, y, 545f, y)
        
        return y - 20
    }
    
    private fun drawStudentInfo(stream: PDPageContentStream, reportCard: StudentReportCard, startY: Float): Float {
        var y = startY
        
        stream.beginText()
        stream.setFont(PDType1Font.HELVETICA_BOLD, 14f)
        stream.newLineAtOffset(50f, y)
        stream.showText("Student Information")
        stream.endText()
        
        y -= 20
        
        stream.beginText()
        stream.setFont(PDType1Font.HELVETICA, 11f)
        stream.newLineAtOffset(50f, y)
        stream.showText("Name: ${reportCard.student.displayNameFr}")
        stream.endText()
        
        y -= 15
        
        stream.beginText()
        stream.newLineAtOffset(50f, y)
        stream.showText("Matricule: ${reportCard.student.displayMatricule}")
        stream.endText()
        
        y -= 15
        
        if (reportCard.student.email != null) {
            stream.beginText()
            stream.newLineAtOffset(50f, y)
            stream.showText("Email: ${reportCard.student.email}")
            stream.endText()
            y -= 15
        }
        
        y -= 10
        drawLine(stream, 50f, y, 545f, y)
        
        return y - 20
    }
    
    private fun drawGradesSummary(stream: PDPageContentStream, summary: GradesSummary, startY: Float): Float {
        var y = startY
        
        stream.beginText()
        stream.setFont(PDType1Font.HELVETICA_BOLD, 14f)
        stream.newLineAtOffset(50f, y)
        stream.showText("Academic Performance")
        stream.endText()
        
        y -= 20
        
        // Overall average
        stream.beginText()
        stream.setFont(PDType1Font.HELVETICA_BOLD, 12f)
        stream.newLineAtOffset(50f, y)
        stream.showText(String.format("Overall Average: %.2f / 20.00", summary.overallAverage))
        stream.endText()
        
        y -= 20
        
        // Category breakdown
        stream.beginText()
        stream.setFont(PDType1Font.HELVETICA, 10f)
        stream.newLineAtOffset(50f, y)
        stream.showText("Category Breakdown:")
        stream.endText()
        
        y -= 15
        
        for ((_, category) in summary.gradesByCategory) {
            stream.beginText()
            stream.setFont(PDType1Font.HELVETICA, 10f)
            stream.newLineAtOffset(70f, y)
            stream.showText(String.format("• %s: %.2f (Weight: %.0f%%)", 
                category.categoryName, category.average, category.weight * 100))
            stream.endText()
            y -= 15
            
            if (y < 100) break // Prevent overflow
        }
        
        y -= 10
        drawLine(stream, 50f, y, 545f, y)
        
        return y - 20
    }
    
    private fun drawAttendanceSummary(stream: PDPageContentStream, summary: AttendanceSummary, startY: Float): Float {
        var y = startY
        
        if (y < 150) return y // Not enough space
        
        stream.beginText()
        stream.setFont(PDType1Font.HELVETICA_BOLD, 14f)
        stream.newLineAtOffset(50f, y)
        stream.showText("Attendance")
        stream.endText()
        
        y -= 20
        
        stream.beginText()
        stream.setFont(PDType1Font.HELVETICA, 10f)
        stream.newLineAtOffset(50f, y)
        stream.showText(String.format("Attendance Rate: %.1f%%", summary.attendanceRate))
        stream.endText()
        
        y -= 15
        
        stream.beginText()
        stream.newLineAtOffset(50f, y)
        stream.showText(String.format("Present: %d | Absent: %d | Late: %d | Excused: %d",
            summary.presentCount, summary.absentCount, summary.lateCount, summary.excusedCount))
        stream.endText()
        
        y -= 20
        drawLine(stream, 50f, y, 545f, y)
        
        return y - 20
    }
    
    private fun drawBehaviorSummary(stream: PDPageContentStream, summary: BehaviorSummary, startY: Float): Float {
        var y = startY
        
        if (y < 100) return y // Not enough space
        
        stream.beginText()
        stream.setFont(PDType1Font.HELVETICA_BOLD, 14f)
        stream.newLineAtOffset(50f, y)
        stream.showText("Behavior")
        stream.endText()
        
        y -= 20
        
        stream.beginText()
        stream.setFont(PDType1Font.HELVETICA, 10f)
        stream.newLineAtOffset(50f, y)
        stream.showText(String.format("Total Points: %d | Positive: %d | Negative: %d",
            summary.totalPoints, summary.positiveCount, summary.negativeCount))
        stream.endText()
        
        return y - 20
    }
    
    private fun drawFooter(stream: PDPageContentStream, reportCard: StudentReportCard) {
        stream.beginText()
        stream.setFont(PDType1Font.HELVETICA, 8f)
        stream.newLineAtOffset(50f, 30f)
        stream.showText("Generated by TeacherHub - ${dateFormat.format(Date())}")
        stream.endText()
    }
    
    private fun drawLine(stream: PDPageContentStream, x1: Float, y: Float, x2: Float, y2: Float) {
        stream.moveTo(x1, y)
        stream.lineTo(x2, y2)
        stream.stroke()
    }
}
