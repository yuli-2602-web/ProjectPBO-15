import java.sql.Connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.CallableStatement;
import java.util.InputMismatchException;
import java.util.Scanner;

class DBConnection {
private static final String URL = "jdbc:mysql://localhost:3306/db_kasir";
private static final String USER = "root";
private static final String PASS = "";

public static Connection getConnection() throws Exception {
    return DriverManager.getConnection(URL, USER, PASS);
}
}

abstract class Produk {
private int id;
private String nama;
private int harga;
private int stok;

public Produk(int id, String nama, int harga, int stok) {
    this.id = id;
    this.nama = nama;
    this.harga = harga;
    this.stok = stok;
}

public int getId() { return id; }
public String getNama() { return nama; }
public int getHarga() { return harga; }
public int getStok() { return stok; }

public abstract double hitungDiskon();
}

class Makanan extends Produk {
public Makanan(int id, String nama, int harga, int stok) {
super(id, nama, harga, stok);
}

@Override
public double hitungDiskon() {
    return getHarga() * 0.1;
}
}

class Pakaian extends Produk {
public Pakaian(int id, String nama, int harga, int stok) {
super(id, nama, harga, stok);
}

@Override
public double hitungDiskon() {
    return getHarga() * 0.2;
}
}

public class Main {
public static void main(String[] args) {
Scanner scanner = new Scanner(System.in);
int pilihanUtama = 0;

    do {
        try {
            System.out.println("\n=== MAIN MENU KASIR ===");
            System.out.println("1. Manajemen Produk");
            System.out.println("2. Transaksi Penjualan");
            System.out.println("3. Laporan Penjualan");
            System.out.println("4. Keluar Aplikasi");
            System.out.print("Pilih Menu Utama (1-4): ");
            pilihanUtama = scanner.nextInt();

            switch (pilihanUtama) {
                case 1:
                    int subMenu1 = 0;
                    System.out.println("\n--- SUB MENU 1: MANAJEMEN PRODUK ---");
                    System.out.println("1. Tambah Produk Baru");
                    System.out.println("2. Tampilkan Daftar Stok Produk");
                    System.out.print("Pilih Sub Menu (1-2): ");
                    subMenu1 = scanner.nextInt();
                    scanner.nextLine();

                    if (subMenu1 == 1) {
                        System.out.print("Nama Produk: ");
                        String nama = scanner.nextLine();
                        System.out.print("Harga: ");
                        int harga = scanner.nextInt();
                        System.out.print("Stok Awal: ");
                        int stok = scanner.nextInt();
                        scanner.nextLine();
                        System.out.print("Kategori (Makanan/Pakaian): ");
                        String kat = scanner.nextLine();

                        Connection conn = DBConnection.getConnection();
                        String sql = "INSERT INTO produk (nama_produk, harga, stok, kategori) VALUES (?, ?, ?, ?)";
                        PreparedStatement ps = conn.prepareStatement(sql);
                        ps.setString(1, nama);
                        ps.setInt(2, harga);
                        ps.setInt(3, stok);
                        ps.setString(4, kat);
                        ps.executeUpdate();
                        System.out.println("Produk baru berhasil ditambahkan!");
                        conn.close();
                    } else if (subMenu1 == 2) {
                        Connection conn = DBConnection.getConnection();
                        Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery("SELECT * FROM produk");
                        System.out.println("\nID | Nama Produk | Harga | Stok | Kategori");
                        while (rs.next()) {
                            System.out.println(rs.getInt("id_produk") + " | " + rs.getString("nama_produk") + " | " + rs.getInt("harga") + " | " + rs.getInt("stok") + " | " + rs.getString("kategori"));
                        }
                        conn.close();
                    }
                    break;

                case 2:
                    int subMenu2 = 0;
                    System.out.println("\n--- SUB MENU 2: TRANSAKSI PENJUALAN ---");
                    System.out.println("1. Input Transaksi Baru");
                    System.out.println("2. Pembayaran & Hitung Kembalian");
                    System.out.print("Pilih Sub Menu (1-2): ");
                    subMenu2 = scanner.nextInt();

                    if (subMenu2 == 1) {
                        System.out.print("Masukkan ID Produk yang dibeli: ");
                        int idProd = scanner.nextInt();
                        System.out.print("Masukkan Jumlah Beli: ");
                        int jmBeli = scanner.nextInt();

                        Connection conn = DBConnection.getConnection();
                        Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery("SELECT harga FROM produk WHERE id_produk = " + idProd);
                        
                        if (rs.next()) {
                            int hargaSatuan = rs.getInt("harga");
                            int totalBayar = hargaSatuan * jmBeli;

                            System.out.println("Total Tagihan Anda: Rp " + totalBayar);
                            System.out.print("Masukkan Jumlah Uang Pembayaran: ");
                            int uangMasuk = scanner.nextInt();

                            CallableStatement csTrans = conn.prepareCall("{call sp_tambah_transaksi(?, ?)}");
                            csTrans.setInt(1, totalBayar);
                            csTrans.setInt(2, uangMasuk);
                            csTrans.executeUpdate();

                            Statement stmt2 = conn.createStatement();
                            ResultSet rs2 = stmt2.executeQuery("SELECT MAX(id_transaksi) AS last_id FROM transaksi");
                            if (rs2.next()) {
                                int lastId = rs2.getInt("last_id");
                                String sqlDetail = "INSERT INTO detail_transaksi (id_transaksi, id_produk, jumlah_beli) VALUES (?, ?, ?)";
                                PreparedStatement psDetail = conn.prepareStatement(sqlDetail);
                                psDetail.setInt(1, lastId);
                                psDetail.setInt(2, idProd);
                                psDetail.setInt(3, jmBeli);
                                psDetail.executeUpdate();
                                System.out.println("Transaksi Berhasil Diproses! (Stok otomatis terpotong)");
                            }
                        } else {
                            System.out.println("ID Produk tidak ditemukan!");
                        }
                        conn.close();
                    } else if (subMenu2 == 2) {
                        System.out.print("Masukkan Total Tagihan: ");
                        int tagihan = scanner.nextInt();
                        System.out.print("Masukkan Uang Pembayaran: ");
                        int uang = scanner.nextInt();

                        Connection conn = DBConnection.getConnection();
                        CallableStatement csFunc = conn.prepareCall("{? = call fn_hitung_kembalian(?, ?)}");
                        csFunc.registerOutParameter(1, java.sql.Types.INTEGER);
                        csFunc.setInt(2, uang);
                        csFunc.setInt(3, tagihan);
                        csFunc.execute();
                        
                        int kembalian = csFunc.getInt(1);
                        System.out.println("Uang Kembalian Pelanggan: Rp " + kembalian);
                        conn.close();
                    }
                    break;

                case 3:
                    int subMenu3 = 0;
                    System.out.println("\n--- SUB MENU 3: LAPORAN PENJUALAN ---");
                    System.out.println("1. Lihat Laporan Penjualan Harian");
                    System.out.println("2. Cari Produk Berdasarkan Kategori");
                    System.out.print("Pilih Sub Menu (1-2): ");
                    subMenu3 = scanner.nextInt();
                    scanner.nextLine();

                    if (subMenu3 == 1) {
                        Connection conn = DBConnection.getConnection();
                        Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery("SELECT * FROM v_laporan_penjualan");
                        System.out.println("\nID Trans | Tanggal | Nama Produk | Kategori | Qty | Total Harga");
                        while (rs.next()) {
                            System.out.println(rs.getInt("id_transaksi") + " | " + rs.getTimestamp("tanggal_transaksi") + " | " + rs.getString("nama_produk") + " | " + rs.getString("kategori") + " | " + rs.getInt("jumlah_beli") + " | " + rs.getInt("total_harga"));
                        }
                        conn.close();
                    } else if (subMenu3 == 2) {
                        System.out.print("Masukkan Kategori yang dicari (Makanan/Pakaian): ");
                        String cariKat = scanner.nextLine();
                        
                        if (cariKat.equalsIgnoreCase("Makanan")) {
                            Produk p = new Makanan(1, "Simulasi Makanan", 10000, 10);
                            System.out.println("Sistem mendeteksi objek kelas Makanan. Contoh potensi diskon kategori ini: Rp " + p.hitungDiskon());
                        } else if (cariKat.equalsIgnoreCase("Pakaian")) {
                            Produk p = new Pakaian(2, "Simulasi Pakaian", 10000, 10);
                            System.out.println("Sistem mendeteksi objek kelas Pakaian. Contoh potensi diskon kategori ini: Rp " + p.hitungDiskon());
                        } else {
                            System.out.println("Kategori tidak valid.");
                        }
                    }
                    break;

                case 4:
                    System.out.println("Keluar dari sistem kasir. Terima kasih!");
                    break;

                default:
                    System.out.println("Pilihan tidak valid! Masukkan angka 1-4.");
            }
        } catch (InputMismatchException e) {
            System.out.println("Error: Input harus berupa angka!");
            scanner.nextLine();
        } catch (Exception e) {
            System.out.println("Error Sistem/Database: " + e.getMessage());
        }
    } while (pilihanUtama != 4);

    scanner.close();
}
}