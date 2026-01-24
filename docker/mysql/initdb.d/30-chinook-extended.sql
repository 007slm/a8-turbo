USE `Chinook`;



SET @target_dim := 100000;
SET @target_invoice := 100000;
SET @target_playlist_track := 300000;
SET @target_invoice_line := 500000;
SET @target_order_items := 300000;
SET @lines_per_invoice := 5;
SET @tracks_per_playlist := 5;
SET @items_per_order := 4;

DROP TEMPORARY TABLE IF EXISTS tmp_numbers;
CREATE TEMPORARY TABLE tmp_numbers (idx INT PRIMARY KEY) ENGINE = MEMORY;

INSERT INTO tmp_numbers (idx)
SELECT
    d5.d * 10000 +
    d4.d * 1000 +
    d3.d * 100 +
    d2.d * 10 +
    d1.d AS idx
FROM (SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) AS d1
         CROSS JOIN (SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) AS d2
         CROSS JOIN (SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) AS d3
         CROSS JOIN (SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) AS d4
         CROSS JOIN (SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) AS d5
WHERE d5.d * 10000 +
      d4.d * 1000 +
      d3.d * 100 +
      d2.d * 10 +
      d1.d < 100000
ORDER BY idx;

-- Expand Genre
SET @genre_need := GREATEST(@target_dim - (SELECT COUNT(*) FROM Genre), 0);
SET @genre_id := (SELECT IFNULL(MAX(GenreId), 0) FROM Genre);

INSERT INTO Genre (GenreId, Name)
SELECT
    @genre_id := @genre_id + 1 AS GenreId,
    CONCAT('Genre ', LPAD(@genre_id, 6, '0')) AS Name
FROM tmp_numbers
WHERE @genre_need > 0
ORDER BY idx
LIMIT 100000;

-- Expand MediaType
SET @mediatype_need := GREATEST(@target_dim - (SELECT COUNT(*) FROM MediaType), 0);
SET @mediatype_id := (SELECT IFNULL(MAX(MediaTypeId), 0) FROM MediaType);

INSERT INTO MediaType (MediaTypeId, Name)
SELECT
    @mediatype_id := @mediatype_id + 1 AS MediaTypeId,
    CONCAT('MediaType ', LPAD(@mediatype_id, 6, '0')) AS Name
FROM tmp_numbers
WHERE @mediatype_need > 0
ORDER BY idx
LIMIT 100000;

-- Expand Artist
SET @artist_need := GREATEST(@target_dim - (SELECT COUNT(*) FROM Artist), 0);
SET @artist_id := (SELECT IFNULL(MAX(ArtistId), 0) FROM Artist);

INSERT INTO Artist (ArtistId, Name)
SELECT
    @artist_id := @artist_id + 1 AS ArtistId,
    CONCAT('Artist ', LPAD(@artist_id, 6, '0')) AS Name
FROM tmp_numbers
WHERE @artist_need > 0
ORDER BY idx
LIMIT 100000;

SET @artist_total := (SELECT COUNT(*) FROM Artist);

-- Expand Album
SET @album_need := GREATEST(@target_dim - (SELECT COUNT(*) FROM Album), 0);
SET @album_id := (SELECT IFNULL(MAX(AlbumId), 0) FROM Album);

INSERT INTO Album (AlbumId, Title, ArtistId)
SELECT
    @album_id := @album_id + 1 AS AlbumId,
    CONCAT('Album ', LPAD(@album_id, 6, '0')) AS Title,
    1 + ((@album_id - 1) % @artist_total) AS ArtistId
FROM tmp_numbers
WHERE @album_need > 0
ORDER BY idx
LIMIT 100000;

SET @album_total := (SELECT COUNT(*) FROM Album);
SET @genre_total := (SELECT COUNT(*) FROM Genre);
SET @mediatype_total := (SELECT COUNT(*) FROM MediaType);

-- Expand Track
SET @track_need := GREATEST(@target_dim - (SELECT COUNT(*) FROM Track), 0);
SET @track_id := (SELECT IFNULL(MAX(TrackId), 0) FROM Track);

INSERT INTO Track (TrackId, Name, AlbumId, MediaTypeId, GenreId, Composer, Milliseconds, Bytes, UnitPrice)
SELECT
    @track_id := @track_id + 1 AS TrackId,
    CONCAT('Track ', LPAD(@track_id, 6, '0')) AS Name,
    1 + ((@track_id - 1) % @album_total) AS AlbumId,
    1 + ((@track_id - 1) % @mediatype_total) AS MediaTypeId,
    1 + ((@track_id - 1) % @genre_total) AS GenreId,
    CONCAT('Composer ', (@track_id - 1) % 500) AS Composer,
    180000 + ((@track_id - 1) % 60000) AS Milliseconds,
    4000000 + ((@track_id - 1) % 900000) AS Bytes,
    ROUND(0.79 + ((@track_id - 1) % 15) * 0.21, 2) AS UnitPrice
FROM tmp_numbers
WHERE @track_need > 0
ORDER BY idx
LIMIT 100000;

SET @track_total := (SELECT COUNT(*) FROM Track);

-- Expand Playlist
SET @playlist_need := GREATEST(@target_dim - (SELECT COUNT(*) FROM Playlist), 0);
SET @playlist_id := (SELECT IFNULL(MAX(PlaylistId), 0) FROM Playlist);
SET @playlist_start := @playlist_id;

INSERT INTO Playlist (PlaylistId, Name)
SELECT
    @playlist_id := @playlist_id + 1 AS PlaylistId,
    CONCAT('Playlist ', LPAD(@playlist_id, 6, '0')) AS Name
FROM tmp_numbers
WHERE @playlist_need > 0
ORDER BY idx
LIMIT 100000;

SET @playlist_total := (SELECT COUNT(*) FROM Playlist);
SET @playlist_first_new := @playlist_start + 1;

-- Expand PlaylistTrack primarily for new playlists
INSERT INTO PlaylistTrack (PlaylistId, TrackId)
SELECT
    @playlist_first_new + base.idx AS PlaylistId,
    1 + (((@playlist_first_new + base.idx) + offsets.offset) % @track_total) AS TrackId
FROM (
         SELECT idx
         FROM tmp_numbers
         WHERE idx < @playlist_need
         ORDER BY idx
     ) AS base
         JOIN (
    SELECT 0 AS offset
    UNION ALL SELECT 1
    UNION ALL SELECT 2
    UNION ALL SELECT 3
    UNION ALL SELECT 4
) AS offsets
WHERE @playlist_need > 0;

SET @playlisttrack_total := (SELECT COUNT(*) FROM PlaylistTrack);
SET @playlisttrack_need := GREATEST(@target_playlist_track - @playlisttrack_total, 0);
SET @extra_playlisttrack_seed := (SELECT IFNULL(MAX(PlaylistId), 0) FROM PlaylistTrack);

INSERT INTO PlaylistTrack (PlaylistId, TrackId)
SELECT
    1 + ((@extra_playlisttrack_seed + idx) % @playlist_total) AS PlaylistId,
    1 + ((@extra_playlisttrack_seed + idx * 7) % @track_total) AS TrackId
FROM tmp_numbers
WHERE @playlisttrack_need > 0
ORDER BY idx
LIMIT 100000;

SET @playlisttrack_total := (SELECT COUNT(*) FROM PlaylistTrack);

-- Expand Employee
SET @employee_need := GREATEST(@target_dim - (SELECT COUNT(*) FROM Employee), 0);
SET @employee_id := (SELECT IFNULL(MAX(EmployeeId), 0) FROM Employee);

INSERT INTO Employee (EmployeeId, LastName, FirstName, Title, ReportsTo, BirthDate, HireDate,
                      Address, City, State, Country, PostalCode, Phone, Fax, Email)
SELECT
    @employee_id := @employee_id + 1 AS EmployeeId,
    CONCAT('Lastname', LPAD(@employee_id, 5, '0')) AS LastName,
    CONCAT('Firstname', LPAD(@employee_id, 5, '0')) AS FirstName,
    CASE WHEN (@employee_id % 7) = 0 THEN 'IT Manager' ELSE 'Sales Representative' END AS Title,
    NULL AS ReportsTo,
    DATE_ADD('1965-01-01', INTERVAL (@employee_id % 18000) DAY) AS BirthDate,
    DATE_ADD('1995-01-01', INTERVAL (@employee_id % 9000) DAY) AS HireDate,
    CONCAT('Employee Address ', @employee_id) AS Address,
    CASE (@employee_id % 6)
        WHEN 0 THEN 'New York'
        WHEN 1 THEN 'London'
        WHEN 2 THEN 'Rio'
        WHEN 3 THEN 'Sydney'
        WHEN 4 THEN 'Berlin'
        ELSE 'Tokyo'
        END AS City,
    CASE (@employee_id % 4)
        WHEN 0 THEN 'NY'
        WHEN 1 THEN 'CA'
        WHEN 2 THEN 'NSW'
        ELSE 'SP'
        END AS State,
    CASE (@employee_id % 6)
        WHEN 0 THEN 'USA'
        WHEN 1 THEN 'UK'
        WHEN 2 THEN 'Brazil'
        WHEN 3 THEN 'Australia'
        WHEN 4 THEN 'Germany'
        ELSE 'Japan'
        END AS Country,
    LPAD(10000 + (@employee_id % 90000), 5, '0') AS PostalCode,
    CONCAT('+1-555-', LPAD(@employee_id % 10000, 4, '0')) AS Phone,
    CONCAT('+1-555-', LPAD((@employee_id + 3000) % 10000, 4, '0')) AS Fax,
    CONCAT('employee', @employee_id, '@chinook.local') AS Email
FROM tmp_numbers
WHERE @employee_need > 0
ORDER BY idx
LIMIT 100000;

SET @employee_total := (SELECT COUNT(*) FROM Employee);

-- Expand Customer
SET @customer_need := GREATEST(@target_dim - (SELECT COUNT(*) FROM Customer), 0);
SET @customer_id := (SELECT IFNULL(MAX(CustomerId), 0) FROM Customer);

INSERT INTO Customer (CustomerId, FirstName, LastName, Company, Address, City, State,
                      Country, PostalCode, Phone, Fax, Email, SupportRepId)
SELECT
    @customer_id := @customer_id + 1 AS CustomerId,
    CONCAT('First', LPAD(@customer_id, 5, '0')) AS FirstName,
    CONCAT('Last', LPAD(@customer_id, 5, '0')) AS LastName,
    CONCAT('Company ', LPAD(@customer_id, 5, '0')) AS Company,
    CONCAT('Customer Address ', @customer_id) AS Address,
    CASE (@customer_id % 6)
        WHEN 0 THEN 'Los Angeles'
        WHEN 1 THEN 'Chicago'
        WHEN 2 THEN 'Toronto'
        WHEN 3 THEN 'Paris'
        WHEN 4 THEN 'Madrid'
        ELSE 'Rome'
        END AS City,
    CASE (@customer_id % 4)
        WHEN 0 THEN 'CA'
        WHEN 1 THEN 'IL'
        WHEN 2 THEN 'ON'
        ELSE 'PA'
        END AS State,
    CASE (@customer_id % 6)
        WHEN 0 THEN 'USA'
        WHEN 1 THEN 'USA'
        WHEN 2 THEN 'Canada'
        WHEN 3 THEN 'France'
        WHEN 4 THEN 'Spain'
        ELSE 'Italy'
        END AS Country,
    LPAD(20000 + (@customer_id % 70000), 5, '0') AS PostalCode,
    CONCAT('+1-666-', LPAD(@customer_id % 10000, 4, '0')) AS Phone,
    CONCAT('+1-666-', LPAD((@customer_id + 4000) % 10000, 4, '0')) AS Fax,
    CONCAT('customer', @customer_id, '@chinook.local') AS Email,
    1 + ((@customer_id - 1) % @employee_total) AS SupportRepId
FROM tmp_numbers
WHERE @customer_need > 0
ORDER BY idx
LIMIT 100000;

SET @customer_total := (SELECT COUNT(*) FROM Customer);

-- Expand Invoice
SET @invoice_need := GREATEST(@target_invoice - (SELECT COUNT(*) FROM Invoice), 0);
SET @invoice_id := (SELECT IFNULL(MAX(InvoiceId), 0) FROM Invoice);
SET @invoice_start := @invoice_id;

INSERT INTO Invoice (InvoiceId, CustomerId, InvoiceDate, BillingAddress, BillingCity,
                     BillingState, BillingCountry, BillingPostalCode, Total)
SELECT
    @invoice_id := @invoice_id + 1 AS InvoiceId,
    1 + ((@invoice_id - 1) % @customer_total) AS CustomerId,
    DATE_ADD('2015-01-01', INTERVAL (@invoice_id - 1) DAY) AS InvoiceDate,
    CONCAT('Invoice Address ', @invoice_id) AS BillingAddress,
    CASE (@invoice_id % 6)
        WHEN 0 THEN 'New York'
        WHEN 1 THEN 'Seattle'
        WHEN 2 THEN 'Vancouver'
        WHEN 3 THEN 'London'
        WHEN 4 THEN 'Lisbon'
        ELSE 'Prague'
        END AS BillingCity,
    CASE (@invoice_id % 4)
        WHEN 0 THEN 'NY'
        WHEN 1 THEN 'WA'
        WHEN 2 THEN 'BC'
        ELSE 'LN'
        END AS BillingState,
    CASE (@invoice_id % 6)
        WHEN 0 THEN 'USA'
        WHEN 1 THEN 'USA'
        WHEN 2 THEN 'Canada'
        WHEN 3 THEN 'UK'
        WHEN 4 THEN 'Portugal'
        ELSE 'Czechia'
        END AS BillingCountry,
    LPAD(30000 + (@invoice_id % 60000), 5, '0') AS BillingPostalCode,
    ROUND(40 + ((@invoice_id % 120) * 3.15), 2) AS Total
FROM tmp_numbers
WHERE @invoice_need > 0
ORDER BY idx
LIMIT 100000;

SET @invoice_total := (SELECT COUNT(*) FROM Invoice);

-- Expand InvoiceLine for new invoices
SET @invoice_line_id := (SELECT IFNULL(MAX(InvoiceLineId), 0) FROM InvoiceLine);
SET @first_new_invoice := @invoice_start + 1;

INSERT INTO InvoiceLine (InvoiceLineId, InvoiceId, TrackId, UnitPrice, Quantity)
SELECT
    @invoice_line_id := @invoice_line_id + 1 AS InvoiceLineId,
    inv.InvoiceId,
    1 + ((inv.InvoiceId + line_no) % @track_total) AS TrackId,
    ROUND(0.79 + ((inv.InvoiceId + line_no) % 20) * 0.19, 2) AS UnitPrice,
    1 + ((inv.InvoiceId + line_no) % 5) AS Quantity
FROM (
         SELECT @first_new_invoice + idx AS InvoiceId
         FROM tmp_numbers
         WHERE idx < @invoice_need
         ORDER BY idx
     ) AS inv
         JOIN (
    SELECT 0 AS line_no
    UNION ALL SELECT 1
    UNION ALL SELECT 2
    UNION ALL SELECT 3
    UNION ALL SELECT 4
) AS line_numbers
WHERE @invoice_need > 0
ORDER BY inv.InvoiceId, line_no;

-- Update totals for new invoices to match invoice lines
UPDATE Invoice AS i
SET Total = (
    SELECT COALESCE(ROUND(SUM(il.UnitPrice * il.Quantity), 2), 0)
    FROM InvoiceLine AS il
    WHERE il.InvoiceId = i.InvoiceId
)
WHERE i.InvoiceId >= @first_new_invoice;

SET @invoice_line_total := (SELECT COUNT(*) FROM InvoiceLine);
SET @invoice_line_remaining := GREATEST(@target_invoice_line - @invoice_line_total, 0);

INSERT INTO InvoiceLine (InvoiceLineId, InvoiceId, TrackId, UnitPrice, Quantity)
SELECT
    @invoice_line_id := @invoice_line_id + 1 AS InvoiceLineId,
    1 + ((@invoice_line_id - 1) % @invoice_total) AS InvoiceId,
    1 + ((@invoice_line_id + idx) % @track_total) AS TrackId,
    ROUND(0.69 + ((@invoice_line_id + idx) % 17) * 0.22, 2) AS UnitPrice,
    1 + ((@invoice_line_id + idx) % 6) AS Quantity
FROM tmp_numbers
WHERE @invoice_line_remaining > 0
ORDER BY idx
LIMIT 100000;

UPDATE Invoice AS i
SET Total = (
    SELECT ROUND(SUM(il.UnitPrice * il.Quantity), 2)
    FROM InvoiceLine AS il
    WHERE il.InvoiceId = i.InvoiceId
)
WHERE EXISTS (
    SELECT 1
    FROM InvoiceLine AS il
    WHERE il.InvoiceId = i.InvoiceId
      AND il.InvoiceLineId > @invoice_line_id - @invoice_line_remaining
);



DROP TEMPORARY TABLE IF EXISTS tmp_numbers;
