package com.nguyendevs.ecolens.database;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.nguyendevs.ecolens.model.HistoryEntry;
import com.nguyendevs.ecolens.model.SpeciesInfo;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class HistoryDao_Impl implements HistoryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<HistoryEntry> __insertionAdapterOfHistoryEntry;

  private final EntityDeletionOrUpdateAdapter<HistoryEntry> __updateAdapterOfHistoryEntry;

  private final SharedSQLiteStatement __preparedStmtOfUpdateSpeciesDetails;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public HistoryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHistoryEntry = new EntityInsertionAdapter<HistoryEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `history_table` (`id`,`imagePath`,`timestamp`,`isFavorite`,`commonName`,`scientificName`,`kingdom`,`phylum`,`className`,`taxorder`,`family`,`genus`,`species`,`description`,`characteristics`,`distribution`,`habitat`,`conservationStatus`,`confidence`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HistoryEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getImagePath());
        statement.bindLong(3, entity.getTimestamp());
        final int _tmp = entity.isFavorite() ? 1 : 0;
        statement.bindLong(4, _tmp);
        final SpeciesInfo _tmpSpeciesInfo = entity.getSpeciesInfo();
        statement.bindString(5, _tmpSpeciesInfo.getCommonName());
        statement.bindString(6, _tmpSpeciesInfo.getScientificName());
        statement.bindString(7, _tmpSpeciesInfo.getKingdom());
        statement.bindString(8, _tmpSpeciesInfo.getPhylum());
        statement.bindString(9, _tmpSpeciesInfo.getClassName());
        statement.bindString(10, _tmpSpeciesInfo.getTaxorder());
        statement.bindString(11, _tmpSpeciesInfo.getFamily());
        statement.bindString(12, _tmpSpeciesInfo.getGenus());
        statement.bindString(13, _tmpSpeciesInfo.getSpecies());
        statement.bindString(14, _tmpSpeciesInfo.getDescription());
        statement.bindString(15, _tmpSpeciesInfo.getCharacteristics());
        statement.bindString(16, _tmpSpeciesInfo.getDistribution());
        statement.bindString(17, _tmpSpeciesInfo.getHabitat());
        statement.bindString(18, _tmpSpeciesInfo.getConservationStatus());
        statement.bindDouble(19, _tmpSpeciesInfo.getConfidence());
      }
    };
    this.__updateAdapterOfHistoryEntry = new EntityDeletionOrUpdateAdapter<HistoryEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `history_table` SET `id` = ?,`imagePath` = ?,`timestamp` = ?,`isFavorite` = ?,`commonName` = ?,`scientificName` = ?,`kingdom` = ?,`phylum` = ?,`className` = ?,`taxorder` = ?,`family` = ?,`genus` = ?,`species` = ?,`description` = ?,`characteristics` = ?,`distribution` = ?,`habitat` = ?,`conservationStatus` = ?,`confidence` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HistoryEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getImagePath());
        statement.bindLong(3, entity.getTimestamp());
        final int _tmp = entity.isFavorite() ? 1 : 0;
        statement.bindLong(4, _tmp);
        final SpeciesInfo _tmpSpeciesInfo = entity.getSpeciesInfo();
        statement.bindString(5, _tmpSpeciesInfo.getCommonName());
        statement.bindString(6, _tmpSpeciesInfo.getScientificName());
        statement.bindString(7, _tmpSpeciesInfo.getKingdom());
        statement.bindString(8, _tmpSpeciesInfo.getPhylum());
        statement.bindString(9, _tmpSpeciesInfo.getClassName());
        statement.bindString(10, _tmpSpeciesInfo.getTaxorder());
        statement.bindString(11, _tmpSpeciesInfo.getFamily());
        statement.bindString(12, _tmpSpeciesInfo.getGenus());
        statement.bindString(13, _tmpSpeciesInfo.getSpecies());
        statement.bindString(14, _tmpSpeciesInfo.getDescription());
        statement.bindString(15, _tmpSpeciesInfo.getCharacteristics());
        statement.bindString(16, _tmpSpeciesInfo.getDistribution());
        statement.bindString(17, _tmpSpeciesInfo.getHabitat());
        statement.bindString(18, _tmpSpeciesInfo.getConservationStatus());
        statement.bindDouble(19, _tmpSpeciesInfo.getConfidence());
        statement.bindLong(20, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateSpeciesDetails = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        UPDATE history_table \n"
                + "        SET commonName = ?,\n"
                + "            scientificName = ?,\n"
                + "            kingdom = ?,\n"
                + "            phylum = ?,\n"
                + "            className = ?,\n"
                + "            taxorder = ?,\n"
                + "            family = ?,\n"
                + "            genus = ?,\n"
                + "            species = ?,\n"
                + "            description = ?,\n"
                + "            characteristics = ?,\n"
                + "            distribution = ?,\n"
                + "            habitat = ?,\n"
                + "            conservationStatus = ?,\n"
                + "            confidence = ?,\n"
                + "            timestamp = ?\n"
                + "        WHERE id = ?\n"
                + "    ";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM history_table";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final HistoryEntry entry, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfHistoryEntry.insertAndReturnId(entry);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final HistoryEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfHistoryEntry.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSpeciesDetails(final int id, final String commonName,
      final String scientificName, final String kingdom, final String phylum,
      final String className, final String taxorder, final String family, final String genus,
      final String species, final String description, final String characteristics,
      final String distribution, final String habitat, final String conservationStatus,
      final double confidence, final long timestamp, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateSpeciesDetails.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, commonName);
        _argIndex = 2;
        _stmt.bindString(_argIndex, scientificName);
        _argIndex = 3;
        _stmt.bindString(_argIndex, kingdom);
        _argIndex = 4;
        _stmt.bindString(_argIndex, phylum);
        _argIndex = 5;
        _stmt.bindString(_argIndex, className);
        _argIndex = 6;
        _stmt.bindString(_argIndex, taxorder);
        _argIndex = 7;
        _stmt.bindString(_argIndex, family);
        _argIndex = 8;
        _stmt.bindString(_argIndex, genus);
        _argIndex = 9;
        _stmt.bindString(_argIndex, species);
        _argIndex = 10;
        _stmt.bindString(_argIndex, description);
        _argIndex = 11;
        _stmt.bindString(_argIndex, characteristics);
        _argIndex = 12;
        _stmt.bindString(_argIndex, distribution);
        _argIndex = 13;
        _stmt.bindString(_argIndex, habitat);
        _argIndex = 14;
        _stmt.bindString(_argIndex, conservationStatus);
        _argIndex = 15;
        _stmt.bindDouble(_argIndex, confidence);
        _argIndex = 16;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 17;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateSpeciesDetails.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<HistoryEntry>> getAllHistoryNewestFirst() {
    final String _sql = "SELECT * FROM history_table ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"history_table"}, new Callable<List<HistoryEntry>>() {
      @Override
      @NonNull
      public List<HistoryEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "imagePath");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfCommonName = CursorUtil.getColumnIndexOrThrow(_cursor, "commonName");
          final int _cursorIndexOfScientificName = CursorUtil.getColumnIndexOrThrow(_cursor, "scientificName");
          final int _cursorIndexOfKingdom = CursorUtil.getColumnIndexOrThrow(_cursor, "kingdom");
          final int _cursorIndexOfPhylum = CursorUtil.getColumnIndexOrThrow(_cursor, "phylum");
          final int _cursorIndexOfClassName = CursorUtil.getColumnIndexOrThrow(_cursor, "className");
          final int _cursorIndexOfTaxorder = CursorUtil.getColumnIndexOrThrow(_cursor, "taxorder");
          final int _cursorIndexOfFamily = CursorUtil.getColumnIndexOrThrow(_cursor, "family");
          final int _cursorIndexOfGenus = CursorUtil.getColumnIndexOrThrow(_cursor, "genus");
          final int _cursorIndexOfSpecies = CursorUtil.getColumnIndexOrThrow(_cursor, "species");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCharacteristics = CursorUtil.getColumnIndexOrThrow(_cursor, "characteristics");
          final int _cursorIndexOfDistribution = CursorUtil.getColumnIndexOrThrow(_cursor, "distribution");
          final int _cursorIndexOfHabitat = CursorUtil.getColumnIndexOrThrow(_cursor, "habitat");
          final int _cursorIndexOfConservationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "conservationStatus");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final List<HistoryEntry> _result = new ArrayList<HistoryEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HistoryEntry _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpImagePath;
            _tmpImagePath = _cursor.getString(_cursorIndexOfImagePath);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final SpeciesInfo _tmpSpeciesInfo;
            final String _tmpCommonName;
            _tmpCommonName = _cursor.getString(_cursorIndexOfCommonName);
            final String _tmpScientificName;
            _tmpScientificName = _cursor.getString(_cursorIndexOfScientificName);
            final String _tmpKingdom;
            _tmpKingdom = _cursor.getString(_cursorIndexOfKingdom);
            final String _tmpPhylum;
            _tmpPhylum = _cursor.getString(_cursorIndexOfPhylum);
            final String _tmpClassName;
            _tmpClassName = _cursor.getString(_cursorIndexOfClassName);
            final String _tmpTaxorder;
            _tmpTaxorder = _cursor.getString(_cursorIndexOfTaxorder);
            final String _tmpFamily;
            _tmpFamily = _cursor.getString(_cursorIndexOfFamily);
            final String _tmpGenus;
            _tmpGenus = _cursor.getString(_cursorIndexOfGenus);
            final String _tmpSpecies;
            _tmpSpecies = _cursor.getString(_cursorIndexOfSpecies);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpCharacteristics;
            _tmpCharacteristics = _cursor.getString(_cursorIndexOfCharacteristics);
            final String _tmpDistribution;
            _tmpDistribution = _cursor.getString(_cursorIndexOfDistribution);
            final String _tmpHabitat;
            _tmpHabitat = _cursor.getString(_cursorIndexOfHabitat);
            final String _tmpConservationStatus;
            _tmpConservationStatus = _cursor.getString(_cursorIndexOfConservationStatus);
            final double _tmpConfidence;
            _tmpConfidence = _cursor.getDouble(_cursorIndexOfConfidence);
            _tmpSpeciesInfo = new SpeciesInfo(_tmpCommonName,_tmpScientificName,_tmpKingdom,_tmpPhylum,_tmpClassName,_tmpTaxorder,_tmpFamily,_tmpGenus,_tmpSpecies,_tmpDescription,_tmpCharacteristics,_tmpDistribution,_tmpHabitat,_tmpConservationStatus,_tmpConfidence);
            _item = new HistoryEntry(_tmpId,_tmpImagePath,_tmpSpeciesInfo,_tmpTimestamp,_tmpIsFavorite);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<HistoryEntry>> getAllHistoryOldestFirst() {
    final String _sql = "SELECT * FROM history_table ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"history_table"}, new Callable<List<HistoryEntry>>() {
      @Override
      @NonNull
      public List<HistoryEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "imagePath");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfCommonName = CursorUtil.getColumnIndexOrThrow(_cursor, "commonName");
          final int _cursorIndexOfScientificName = CursorUtil.getColumnIndexOrThrow(_cursor, "scientificName");
          final int _cursorIndexOfKingdom = CursorUtil.getColumnIndexOrThrow(_cursor, "kingdom");
          final int _cursorIndexOfPhylum = CursorUtil.getColumnIndexOrThrow(_cursor, "phylum");
          final int _cursorIndexOfClassName = CursorUtil.getColumnIndexOrThrow(_cursor, "className");
          final int _cursorIndexOfTaxorder = CursorUtil.getColumnIndexOrThrow(_cursor, "taxorder");
          final int _cursorIndexOfFamily = CursorUtil.getColumnIndexOrThrow(_cursor, "family");
          final int _cursorIndexOfGenus = CursorUtil.getColumnIndexOrThrow(_cursor, "genus");
          final int _cursorIndexOfSpecies = CursorUtil.getColumnIndexOrThrow(_cursor, "species");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCharacteristics = CursorUtil.getColumnIndexOrThrow(_cursor, "characteristics");
          final int _cursorIndexOfDistribution = CursorUtil.getColumnIndexOrThrow(_cursor, "distribution");
          final int _cursorIndexOfHabitat = CursorUtil.getColumnIndexOrThrow(_cursor, "habitat");
          final int _cursorIndexOfConservationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "conservationStatus");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final List<HistoryEntry> _result = new ArrayList<HistoryEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HistoryEntry _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpImagePath;
            _tmpImagePath = _cursor.getString(_cursorIndexOfImagePath);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final SpeciesInfo _tmpSpeciesInfo;
            final String _tmpCommonName;
            _tmpCommonName = _cursor.getString(_cursorIndexOfCommonName);
            final String _tmpScientificName;
            _tmpScientificName = _cursor.getString(_cursorIndexOfScientificName);
            final String _tmpKingdom;
            _tmpKingdom = _cursor.getString(_cursorIndexOfKingdom);
            final String _tmpPhylum;
            _tmpPhylum = _cursor.getString(_cursorIndexOfPhylum);
            final String _tmpClassName;
            _tmpClassName = _cursor.getString(_cursorIndexOfClassName);
            final String _tmpTaxorder;
            _tmpTaxorder = _cursor.getString(_cursorIndexOfTaxorder);
            final String _tmpFamily;
            _tmpFamily = _cursor.getString(_cursorIndexOfFamily);
            final String _tmpGenus;
            _tmpGenus = _cursor.getString(_cursorIndexOfGenus);
            final String _tmpSpecies;
            _tmpSpecies = _cursor.getString(_cursorIndexOfSpecies);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpCharacteristics;
            _tmpCharacteristics = _cursor.getString(_cursorIndexOfCharacteristics);
            final String _tmpDistribution;
            _tmpDistribution = _cursor.getString(_cursorIndexOfDistribution);
            final String _tmpHabitat;
            _tmpHabitat = _cursor.getString(_cursorIndexOfHabitat);
            final String _tmpConservationStatus;
            _tmpConservationStatus = _cursor.getString(_cursorIndexOfConservationStatus);
            final double _tmpConfidence;
            _tmpConfidence = _cursor.getDouble(_cursorIndexOfConfidence);
            _tmpSpeciesInfo = new SpeciesInfo(_tmpCommonName,_tmpScientificName,_tmpKingdom,_tmpPhylum,_tmpClassName,_tmpTaxorder,_tmpFamily,_tmpGenus,_tmpSpecies,_tmpDescription,_tmpCharacteristics,_tmpDistribution,_tmpHabitat,_tmpConservationStatus,_tmpConfidence);
            _item = new HistoryEntry(_tmpId,_tmpImagePath,_tmpSpeciesInfo,_tmpTimestamp,_tmpIsFavorite);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getHistoryById(final int id, final Continuation<? super HistoryEntry> $completion) {
    final String _sql = "SELECT * FROM history_table WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<HistoryEntry>() {
      @Override
      @Nullable
      public HistoryEntry call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "imagePath");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfCommonName = CursorUtil.getColumnIndexOrThrow(_cursor, "commonName");
          final int _cursorIndexOfScientificName = CursorUtil.getColumnIndexOrThrow(_cursor, "scientificName");
          final int _cursorIndexOfKingdom = CursorUtil.getColumnIndexOrThrow(_cursor, "kingdom");
          final int _cursorIndexOfPhylum = CursorUtil.getColumnIndexOrThrow(_cursor, "phylum");
          final int _cursorIndexOfClassName = CursorUtil.getColumnIndexOrThrow(_cursor, "className");
          final int _cursorIndexOfTaxorder = CursorUtil.getColumnIndexOrThrow(_cursor, "taxorder");
          final int _cursorIndexOfFamily = CursorUtil.getColumnIndexOrThrow(_cursor, "family");
          final int _cursorIndexOfGenus = CursorUtil.getColumnIndexOrThrow(_cursor, "genus");
          final int _cursorIndexOfSpecies = CursorUtil.getColumnIndexOrThrow(_cursor, "species");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCharacteristics = CursorUtil.getColumnIndexOrThrow(_cursor, "characteristics");
          final int _cursorIndexOfDistribution = CursorUtil.getColumnIndexOrThrow(_cursor, "distribution");
          final int _cursorIndexOfHabitat = CursorUtil.getColumnIndexOrThrow(_cursor, "habitat");
          final int _cursorIndexOfConservationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "conservationStatus");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final HistoryEntry _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpImagePath;
            _tmpImagePath = _cursor.getString(_cursorIndexOfImagePath);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final SpeciesInfo _tmpSpeciesInfo;
            final String _tmpCommonName;
            _tmpCommonName = _cursor.getString(_cursorIndexOfCommonName);
            final String _tmpScientificName;
            _tmpScientificName = _cursor.getString(_cursorIndexOfScientificName);
            final String _tmpKingdom;
            _tmpKingdom = _cursor.getString(_cursorIndexOfKingdom);
            final String _tmpPhylum;
            _tmpPhylum = _cursor.getString(_cursorIndexOfPhylum);
            final String _tmpClassName;
            _tmpClassName = _cursor.getString(_cursorIndexOfClassName);
            final String _tmpTaxorder;
            _tmpTaxorder = _cursor.getString(_cursorIndexOfTaxorder);
            final String _tmpFamily;
            _tmpFamily = _cursor.getString(_cursorIndexOfFamily);
            final String _tmpGenus;
            _tmpGenus = _cursor.getString(_cursorIndexOfGenus);
            final String _tmpSpecies;
            _tmpSpecies = _cursor.getString(_cursorIndexOfSpecies);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpCharacteristics;
            _tmpCharacteristics = _cursor.getString(_cursorIndexOfCharacteristics);
            final String _tmpDistribution;
            _tmpDistribution = _cursor.getString(_cursorIndexOfDistribution);
            final String _tmpHabitat;
            _tmpHabitat = _cursor.getString(_cursorIndexOfHabitat);
            final String _tmpConservationStatus;
            _tmpConservationStatus = _cursor.getString(_cursorIndexOfConservationStatus);
            final double _tmpConfidence;
            _tmpConfidence = _cursor.getDouble(_cursorIndexOfConfidence);
            _tmpSpeciesInfo = new SpeciesInfo(_tmpCommonName,_tmpScientificName,_tmpKingdom,_tmpPhylum,_tmpClassName,_tmpTaxorder,_tmpFamily,_tmpGenus,_tmpSpecies,_tmpDescription,_tmpCharacteristics,_tmpDistribution,_tmpHabitat,_tmpConservationStatus,_tmpConfidence);
            _result = new HistoryEntry(_tmpId,_tmpImagePath,_tmpSpeciesInfo,_tmpTimestamp,_tmpIsFavorite);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<HistoryEntry>> getHistoryByDateRangeNewest(final long startDate,
      final long endDate) {
    final String _sql = "SELECT * FROM history_table WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"history_table"}, new Callable<List<HistoryEntry>>() {
      @Override
      @NonNull
      public List<HistoryEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "imagePath");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfCommonName = CursorUtil.getColumnIndexOrThrow(_cursor, "commonName");
          final int _cursorIndexOfScientificName = CursorUtil.getColumnIndexOrThrow(_cursor, "scientificName");
          final int _cursorIndexOfKingdom = CursorUtil.getColumnIndexOrThrow(_cursor, "kingdom");
          final int _cursorIndexOfPhylum = CursorUtil.getColumnIndexOrThrow(_cursor, "phylum");
          final int _cursorIndexOfClassName = CursorUtil.getColumnIndexOrThrow(_cursor, "className");
          final int _cursorIndexOfTaxorder = CursorUtil.getColumnIndexOrThrow(_cursor, "taxorder");
          final int _cursorIndexOfFamily = CursorUtil.getColumnIndexOrThrow(_cursor, "family");
          final int _cursorIndexOfGenus = CursorUtil.getColumnIndexOrThrow(_cursor, "genus");
          final int _cursorIndexOfSpecies = CursorUtil.getColumnIndexOrThrow(_cursor, "species");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCharacteristics = CursorUtil.getColumnIndexOrThrow(_cursor, "characteristics");
          final int _cursorIndexOfDistribution = CursorUtil.getColumnIndexOrThrow(_cursor, "distribution");
          final int _cursorIndexOfHabitat = CursorUtil.getColumnIndexOrThrow(_cursor, "habitat");
          final int _cursorIndexOfConservationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "conservationStatus");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final List<HistoryEntry> _result = new ArrayList<HistoryEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HistoryEntry _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpImagePath;
            _tmpImagePath = _cursor.getString(_cursorIndexOfImagePath);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final SpeciesInfo _tmpSpeciesInfo;
            final String _tmpCommonName;
            _tmpCommonName = _cursor.getString(_cursorIndexOfCommonName);
            final String _tmpScientificName;
            _tmpScientificName = _cursor.getString(_cursorIndexOfScientificName);
            final String _tmpKingdom;
            _tmpKingdom = _cursor.getString(_cursorIndexOfKingdom);
            final String _tmpPhylum;
            _tmpPhylum = _cursor.getString(_cursorIndexOfPhylum);
            final String _tmpClassName;
            _tmpClassName = _cursor.getString(_cursorIndexOfClassName);
            final String _tmpTaxorder;
            _tmpTaxorder = _cursor.getString(_cursorIndexOfTaxorder);
            final String _tmpFamily;
            _tmpFamily = _cursor.getString(_cursorIndexOfFamily);
            final String _tmpGenus;
            _tmpGenus = _cursor.getString(_cursorIndexOfGenus);
            final String _tmpSpecies;
            _tmpSpecies = _cursor.getString(_cursorIndexOfSpecies);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpCharacteristics;
            _tmpCharacteristics = _cursor.getString(_cursorIndexOfCharacteristics);
            final String _tmpDistribution;
            _tmpDistribution = _cursor.getString(_cursorIndexOfDistribution);
            final String _tmpHabitat;
            _tmpHabitat = _cursor.getString(_cursorIndexOfHabitat);
            final String _tmpConservationStatus;
            _tmpConservationStatus = _cursor.getString(_cursorIndexOfConservationStatus);
            final double _tmpConfidence;
            _tmpConfidence = _cursor.getDouble(_cursorIndexOfConfidence);
            _tmpSpeciesInfo = new SpeciesInfo(_tmpCommonName,_tmpScientificName,_tmpKingdom,_tmpPhylum,_tmpClassName,_tmpTaxorder,_tmpFamily,_tmpGenus,_tmpSpecies,_tmpDescription,_tmpCharacteristics,_tmpDistribution,_tmpHabitat,_tmpConservationStatus,_tmpConfidence);
            _item = new HistoryEntry(_tmpId,_tmpImagePath,_tmpSpeciesInfo,_tmpTimestamp,_tmpIsFavorite);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<HistoryEntry>> getHistoryByDateRangeOldest(final long startDate,
      final long endDate) {
    final String _sql = "SELECT * FROM history_table WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"history_table"}, new Callable<List<HistoryEntry>>() {
      @Override
      @NonNull
      public List<HistoryEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "imagePath");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfCommonName = CursorUtil.getColumnIndexOrThrow(_cursor, "commonName");
          final int _cursorIndexOfScientificName = CursorUtil.getColumnIndexOrThrow(_cursor, "scientificName");
          final int _cursorIndexOfKingdom = CursorUtil.getColumnIndexOrThrow(_cursor, "kingdom");
          final int _cursorIndexOfPhylum = CursorUtil.getColumnIndexOrThrow(_cursor, "phylum");
          final int _cursorIndexOfClassName = CursorUtil.getColumnIndexOrThrow(_cursor, "className");
          final int _cursorIndexOfTaxorder = CursorUtil.getColumnIndexOrThrow(_cursor, "taxorder");
          final int _cursorIndexOfFamily = CursorUtil.getColumnIndexOrThrow(_cursor, "family");
          final int _cursorIndexOfGenus = CursorUtil.getColumnIndexOrThrow(_cursor, "genus");
          final int _cursorIndexOfSpecies = CursorUtil.getColumnIndexOrThrow(_cursor, "species");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCharacteristics = CursorUtil.getColumnIndexOrThrow(_cursor, "characteristics");
          final int _cursorIndexOfDistribution = CursorUtil.getColumnIndexOrThrow(_cursor, "distribution");
          final int _cursorIndexOfHabitat = CursorUtil.getColumnIndexOrThrow(_cursor, "habitat");
          final int _cursorIndexOfConservationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "conservationStatus");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final List<HistoryEntry> _result = new ArrayList<HistoryEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HistoryEntry _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpImagePath;
            _tmpImagePath = _cursor.getString(_cursorIndexOfImagePath);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final SpeciesInfo _tmpSpeciesInfo;
            final String _tmpCommonName;
            _tmpCommonName = _cursor.getString(_cursorIndexOfCommonName);
            final String _tmpScientificName;
            _tmpScientificName = _cursor.getString(_cursorIndexOfScientificName);
            final String _tmpKingdom;
            _tmpKingdom = _cursor.getString(_cursorIndexOfKingdom);
            final String _tmpPhylum;
            _tmpPhylum = _cursor.getString(_cursorIndexOfPhylum);
            final String _tmpClassName;
            _tmpClassName = _cursor.getString(_cursorIndexOfClassName);
            final String _tmpTaxorder;
            _tmpTaxorder = _cursor.getString(_cursorIndexOfTaxorder);
            final String _tmpFamily;
            _tmpFamily = _cursor.getString(_cursorIndexOfFamily);
            final String _tmpGenus;
            _tmpGenus = _cursor.getString(_cursorIndexOfGenus);
            final String _tmpSpecies;
            _tmpSpecies = _cursor.getString(_cursorIndexOfSpecies);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpCharacteristics;
            _tmpCharacteristics = _cursor.getString(_cursorIndexOfCharacteristics);
            final String _tmpDistribution;
            _tmpDistribution = _cursor.getString(_cursorIndexOfDistribution);
            final String _tmpHabitat;
            _tmpHabitat = _cursor.getString(_cursorIndexOfHabitat);
            final String _tmpConservationStatus;
            _tmpConservationStatus = _cursor.getString(_cursorIndexOfConservationStatus);
            final double _tmpConfidence;
            _tmpConfidence = _cursor.getDouble(_cursorIndexOfConfidence);
            _tmpSpeciesInfo = new SpeciesInfo(_tmpCommonName,_tmpScientificName,_tmpKingdom,_tmpPhylum,_tmpClassName,_tmpTaxorder,_tmpFamily,_tmpGenus,_tmpSpecies,_tmpDescription,_tmpCharacteristics,_tmpDistribution,_tmpHabitat,_tmpConservationStatus,_tmpConfidence);
            _item = new HistoryEntry(_tmpId,_tmpImagePath,_tmpSpeciesInfo,_tmpTimestamp,_tmpIsFavorite);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
