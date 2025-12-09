package com.example.server.attendance;

import com.example.server.course.Course;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

class PdfExporter {

    static byte[] exportCourse(Course course, List<AttendanceSession> sessions,
                               java.util.function.Function<Long, List<AttendanceRecord>> recordsBySession) throws Exception {
        var baos = new ByteArrayOutputStream();
        var doc  = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        var h1 = new Font(Font.HELVETICA, 18, Font.BOLD);
        var h2 = new Font(Font.HELVETICA, 14, Font.BOLD);
        var normal = new Font(Font.HELVETICA, 11, Font.NORMAL);

        Paragraph title = new Paragraph(
                "Ders Yoklama Raporu\n" + course.getCourseName() + " (" + course.getCourseCode() + ")", h1);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(12);
        doc.add(title);

        doc.add(new Paragraph("Toplam oturum sayısı: " + sessions.size(), normal));
        doc.add(new Paragraph("Oluşturulma: " + java.time.ZonedDateTime.now(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), normal));
        doc.add(Chunk.NEWLINE);

        var dt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

        for (var s : sessions) {
            Paragraph p = new Paragraph(
                    (s.getDescription() != null && !s.getDescription().isBlank() ? s.getDescription() : "Oturum")
                            + " — Başlangıç: " + dt.format(s.getCreatedAt())
                            + (s.getExpiresAt() != null ? (" | Bitiş: " + dt.format(s.getExpiresAt())) : "")
                            + " | Durum: " + (s.isActive() ? "Aktif" : "Pasif"),
                    h2
            );
            p.setSpacingBefore(10);
            p.setSpacingAfter(6);
            doc.add(p);

            List<AttendanceRecord> rows = recordsBySession.apply(s.getId());

            if (rows.isEmpty()) {
                Paragraph none = new Paragraph("Bu oturuma katılım yok.", normal);
                none.setSpacingAfter(8);
                doc.add(none);
                continue;
            }

            PdfPTable table = new PdfPTable(new float[]{10f, 30f, 40f, 20f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(4);

            addHeader(table, "No");
            addHeader(table, "Öğrenci No");
            addHeader(table, "Ad Soyad");
            addHeader(table, "Saat");

            int i = 1;
            for (var r : rows) {
                var u = r.getStudent();
                table.addCell(cell(String.valueOf(i++)));
                table.addCell(cell(String.valueOf(u.getUserName())));
                table.addCell(cell(nullToEmpty(u.getName()) + " " + nullToEmpty(u.getSurname())));
                table.addCell(cell(dt.format(r.getCheckedAt())));
            }
            doc.add(table);
        }

        doc.close();
        return baos.toByteArray();
    }

    private static void addHeader(PdfPTable t, String s) {
        var f = new Font(Font.HELVETICA, 11, Font.BOLD);
        PdfPCell c = new PdfPCell(new Phrase(s, f));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(5f);
        t.addCell(c);
    }

    private static PdfPCell cell(String s) {
        PdfPCell c = new PdfPCell(new Phrase(s == null ? "" : s));
        c.setPadding(4f);
        return c;
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
}
