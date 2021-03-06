package com.hida.service;

import com.hida.dao.DefaultSettingDao;
import com.hida.dao.PidDao;
import com.hida.dao.UsedSettingDao;
import com.hida.model.DefaultSetting;
import com.hida.model.NotEnoughPermutationsException;
import com.hida.model.Pid;
import com.hida.model.TokenType;
import com.hida.model.UsedSetting;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * This class tests the functionality of MinterServiceImpl using Mockito.
 *
 * @author lruffin
 */
public class MinterServiceImplTest {

    @Mock
    DefaultSettingDao DefaultSettingDao;

    @Mock
    PidDao PidDao;

    @Mock
    UsedSettingDao UsedSettingDao;

    @InjectMocks
    MinterServiceImpl MinterServiceImpl;

    ArrayList<DefaultSetting> DefaultSettingList = new ArrayList<>();

    Set<Pid> PidSet = new TreeSet<>();

    /**
     * Sets up Mockito
     *
     * @throws Exception
     */
    @BeforeClass
    public void setUpClass() throws Exception {
        MockitoAnnotations.initMocks(this);
        initializeDefaultSettingList();
        initializePidSet();
    }

    /**
     * Test the various mint settings (auto/random and random/sequential)
     *
     * @return An array of values
     */
    @DataProvider(name = "mintSettings")
    public Object[][] mintSettings() {
        return new Object[][]{
            {true, true},
            {true, false},
            {false, true},
            {false, false}
        };
    }

    /**
     * Tests the MinterServiceImpl by assuming that the settings aren't
     * currently stored in the database
     *
     * @param isRandom Determines if the PIDs are created randomly or
     * sequentially
     * @param isAuto Determines which generator, either Auto or Custom, will be
     * used
     */
    @Test(dataProvider = "mintSettings")
    public void testMintWithNewUsedSetting(boolean isRandom, boolean isAuto) {
        // retrieve a sample DefaultSetting entity
        DefaultSetting defaultSetting = DefaultSettingList.get(1);
        defaultSetting.setAuto(isAuto);
        defaultSetting.setRandom(isRandom);

        // assume that any Pids created aren't already persisted and pretend to persist them
        when(PidDao.findByName(any(String.class))).thenReturn(null);
        doNothing().when(PidDao).savePid(any(Pid.class));

        // assume the UsedSetting isn't persisted and pretend to persist it
        when(UsedSettingDao.findUsedSetting(any(UsedSetting.class))).thenReturn(null);
        doNothing().when(UsedSettingDao).save(any(UsedSetting.class));

        // check to see if all the Pids were created
        Set<Pid> testSet = MinterServiceImpl.mint(10, defaultSetting);
        boolean containsAll = testSet.containsAll(PidSet);
        Assert.assertEquals(containsAll, true);
    }

    /**
     * Tests the MinterServiceImpl under the scenario where UsedSetting entity
     * with matching parameters already exist.
     *
     * @param isRandom Determines if the PIDs are created randomly or
     * sequentially
     * @param isAuto Determines which generator, either Auto or Custom, will be
     * used
     */
    @Test(dataProvider = "mintSettings")
    public void testMintWithOldUsedSetting(boolean isAuto, boolean isRandom) {
        // retrieve a sample DefaultSetting entity
        DefaultSetting defaultSetting = DefaultSettingList.get(1);
        defaultSetting.setAuto(isAuto);
        defaultSetting.setRandom(isRandom);
        
        // get a sample UsedSetting entity
        UsedSetting usedSetting = getSampleUsedSetting();

        // assume that any Pids created aren't already persisted and pretend to persist them
        when(PidDao.findByName(any(String.class))).thenReturn(null);
        doNothing().when(PidDao).savePid(any(Pid.class));

        // assume the UsedSetting isn't persisted and pretend to persist it
        when(UsedSettingDao.findUsedSetting(any(UsedSetting.class))).thenReturn(usedSetting);
        doNothing().when(UsedSettingDao).save(any(UsedSetting.class));

        // check to see if all the Pids were created
        Set<Pid> testSet = MinterServiceImpl.mint(5, defaultSetting);
        boolean containsAll = PidSet.containsAll(testSet);
        Assert.assertEquals(containsAll, true);
    }

    /**
     * Tests the MinterServiceImpl to ensure that a
     * NotEnoughPermutationsException is thrown whenever the amount retrieved
     * from FindUsedSetting is less than the requested amount.
     *
     * @param isRandom Determines if the PIDs are created randomly or
     * sequentially
     * @param isAuto Determines which generator, either Auto or Custom, will be
     * used
     */
    @Test(expectedExceptions = NotEnoughPermutationsException.class, dataProvider = "mintSettings")
    public void testMintNotEnoughPermutationsExceptionInFindUsedSetting(
            boolean isAuto, boolean isRandom) {
        // retrieve a sample DefaultSetting entity
        DefaultSetting defaultSetting = DefaultSettingList.get(1);
        defaultSetting.setAuto(isAuto);
        defaultSetting.setRandom(isRandom);

        // get a sample UsedSetting entity
        UsedSetting usedSetting = getSampleUsedSetting();

        // assume that any Pids created aren't already persisted and pretend to persist them
        when(PidDao.findByName(any(String.class))).thenReturn(null);
        doNothing().when(PidDao).savePid(any(Pid.class));

        // pretend to find and retrieve variable usedSetting
        when(UsedSettingDao.findUsedSetting(any(UsedSetting.class))).thenReturn(usedSetting);
        when(UsedSettingDao.findUsedSettingById(anyInt())).thenReturn(usedSetting);

        // try to mint an amount greater than what is available
        Set<Pid> testSet = MinterServiceImpl.mint(6, defaultSetting);
    }

    /**
     * Tests the MinterServiceImpl to ensure that a
     * NotEnoughPermutationsException is thrown whenever the requested amount of
     * Pids to mint exceeds the possible number of permutations.
     *
     * @param isRandom Determines if the PIDs are created randomly or
     * sequentially
     * @param isAuto Determines which generator, either Auto or Custom, will be
     * used
     */
    @Test(expectedExceptions = NotEnoughPermutationsException.class, dataProvider = "mintSettings")
    public void testMintNotEnoughPermutationsExceptionInCalculatePermutations(
            boolean isAuto, boolean isRandom) {
        // retrieve a sample DefaultSetting entity
        DefaultSetting defaultSetting = DefaultSettingList.get(1);
        defaultSetting.setAuto(isAuto);
        defaultSetting.setRandom(isRandom);

        // assume that any Pids created aren't already persisted and pretend to persist them
        when(PidDao.findByName(any(String.class))).thenReturn(null);
        doNothing().when(PidDao).savePid(any(Pid.class));

        // assume that UsedSetting entity with the relevant parameters does not exist
        when(UsedSettingDao.findUsedSetting(any(UsedSetting.class))).thenReturn(null);
        doNothing().when(UsedSettingDao).save(any(UsedSetting.class));

        // try to mint an amount greater than what is possible
        Set<Pid> testSet = MinterServiceImpl.mint(11, defaultSetting);
    }

    /**
     * Tests the MinterServiceImpl to ensure that a
     * NotEnoughPermutationsException is thrown whenever it is no longer
     * possible to 'roll' Pids. This is important because there may be different
     * settings that may have created Pids that could match the fields of the
     * currently used setting.
     *
     * @param isRandom Determines if the PIDs are created randomly or
     * sequentially
     * @param isAuto Determines which generator, either Auto or Custom, will be
     * used
     */
    @Test(expectedExceptions = NotEnoughPermutationsException.class, dataProvider = "mintSettings")
    public void testMintNotEnoughPermutationExceptionInRollId(boolean isAuto, boolean isRandom) {
        // retrieve a sample DefaultSetting entity
        DefaultSetting defaultSetting = DefaultSettingList.get(1);
        defaultSetting.setAuto(isAuto);
        defaultSetting.setRandom(isRandom);

        // pretend any Pid with the name "0" is the only Pid that exists
        when(PidDao.findByName(any(String.class))).thenReturn(null);
        when(PidDao.findByName("0")).thenReturn(new TestPid(0));
        doNothing().when(PidDao).savePid(any(Pid.class));

        // assume that UsedSetting entity with the relevant parameters does not exist
        when(UsedSettingDao.findUsedSetting(any(UsedSetting.class))).thenReturn(null);
        doNothing().when(UsedSettingDao).save(any(UsedSetting.class));

        // try to mint an amount greater than what is possible
        Set<Pid> testSet = MinterServiceImpl.mint(10, defaultSetting);
    }

    /**
     * Test in MinterServiceImpl that ensures that the CurrentSetting is sought
     * after.
     */
    @Test
    public void testGetCurrentSettingWithExistingDefaultSetting() {
        DefaultSetting defaultSetting = DefaultSettingList.get(0);
        when(DefaultSettingDao.getDefaultSetting()).thenReturn(defaultSetting);

        MinterServiceImpl.getCurrentSetting();
        verify(DefaultSettingDao, atLeastOnce()).getDefaultSetting();
    }

    /**
     * Test in MinterServiceImpl that ensures that the CurrentSetting is sought
     * after and if it does not exist, a new DefaultSetting is created and
     * saved.
     */
    @Test
    public void testGetCurrentSettingWithoutExistingDefaultSetting() {
        DefaultSetting defaultSetting = DefaultSettingList.get(0);
        when(DefaultSettingDao.getDefaultSetting()).thenReturn(null);
        DefaultSetting actualSetting = MinterServiceImpl.getCurrentSetting();

        Assert.assertEquals(actualSetting.getCharMap(), defaultSetting.getCharMap());
        Assert.assertEquals(actualSetting.getPrefix(), defaultSetting.getPrefix());
        Assert.assertEquals(actualSetting.getPrepend(), defaultSetting.getPrepend());
        Assert.assertEquals(actualSetting.getRootLength(), defaultSetting.getRootLength());
        Assert.assertEquals(actualSetting.getTokenType(), defaultSetting.getTokenType());
        Assert.assertEquals(actualSetting.isAuto(), defaultSetting.isAuto());
        Assert.assertEquals(actualSetting.isRandom(), defaultSetting.isRandom());
        Assert.assertEquals(actualSetting.isSansVowels(), defaultSetting.isSansVowels());
    }

    /**
     * Test in MinterServiceImpl that checks if CurrentSetting in
     * MinterServiceImpl is being properly updated.
     */
    @Test
    public void testUpdateCurrentSetting() {
        DefaultSetting defaultSetting = DefaultSettingList.get(0);
        when(DefaultSettingDao.getDefaultSetting()).thenReturn(defaultSetting);

        MinterServiceImpl.updateCurrentSetting(defaultSetting);
        verify(DefaultSettingDao, atLeastOnce()).getDefaultSetting();
    }

    /**
     * Create a list of sample DefaultSettingList
     */
    private void initializeDefaultSettingList() {
        DefaultSetting defaultSetting1 = new DefaultSetting("", // prepend
                "", // prefix
                TokenType.DIGIT, // token type
                "ddddd", // charmap
                5, // rootlength
                true, // sans vowel
                true, // is auto
                true); // is random

        DefaultSettingList.add(defaultSetting1);

        DefaultSetting defaultSetting2 = new DefaultSetting("", // prepend
                "", // prefix
                TokenType.DIGIT, // token type
                "d", // charmap
                1, // rootlength
                true, // sans vowel
                true, // is auto
                false); // is random

        DefaultSettingList.add(defaultSetting2);
    }

    /**
     * Create a sample set of Pid
     */
    private void initializePidSet() {
        for (int i = 0; i < 10; i++) {
            PidSet.add(new TestPid(i));
        }
    }

    /**
     * Return a sample UsedSetting
     *
     * @return
     */
    private UsedSetting getSampleUsedSetting() {
        return new UsedSetting("", // prefix
                TokenType.DIGIT, // tokentype
                "d", // charmap
                1, // rootlength
                true, //sans vowels
                5); // amount
    }

    /**
     * A test class used to create Pid
     */
    private class TestPid extends Pid {

        public TestPid(int n) {
            BaseMap = new int[1];
            BaseMap[0] = n;
        }

        @Override
        public boolean incrementId() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
