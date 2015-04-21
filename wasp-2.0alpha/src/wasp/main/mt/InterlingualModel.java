/*
 * Copyright 2006, 2007 Yuk Wah Wong.
 * 
 * This file is part of the WASP distribution.
 *
 * WASP is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * WASP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with WASP; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package wasp.main.mt;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import wasp.data.Example;
import wasp.data.Examples;
import wasp.data.Meaning;
import wasp.main.Config;
import wasp.main.Parse;
import wasp.main.Parser;
import wasp.main.Translator;
import wasp.main.generate.GenerateModel;
import wasp.main.parse.ParseModel;
import wasp.nl.NL;
import wasp.util.Bool;
import wasp.util.Int;

/**
 * The interlingual machine translation model.  Underlying this model are two components: a parser and a
 * generator.  The parser can be any parser, and the generator can be any generator.  Configuration
 * settings for the parser and the generator are defined in separate configuration files.
 * 
 * @see wasp.main.Config#INTERLINGUAL_PARSE_CONFIG
 * @see wasp.main.Config#INTERLINGUAL_GEN_CONFIG
 * @author ywwong
 *
 */
public class InterlingualModel extends MTModel {

	/** Indicates whether to use cross validation when training interlingual MT models.  If this
	 * variable is <code>false</code>, then the generation model will be trained on gold-standard
	 * MRs. */
	private boolean DO_CROSS_VALIDATE;
	private int NUM_FOLDS;
	private boolean TRAIN_PARSE_MODEL = true;
	private boolean TRAIN_GENERATE_MODEL = true;
	
	private String srcNL;
	private String tarNL;
	private String mtConfig;
	private String parseConfig;
	private String genConfig;
	private String mtModelDir;
	private String parseModelDir;
	private String genModelDir;
	private ParseModel pm;
	private GenerateModel gm;
	
	public InterlingualModel() throws IOException {
		DO_CROSS_VALIDATE = Bool.parseBool(Config.get(Config.INTERLINGUAL_CROSS_VALIDATE));
		if (DO_CROSS_VALIDATE) {
			NUM_FOLDS = Int.parseInt(Config.get(Config.INTERLINGUAL_NUM_FOLDS));
			TRAIN_PARSE_MODEL = Bool.parseBool(Config.get(Config.INTERLINGUAL_TRAIN_PARSE_MODEL));
		} else {
			TRAIN_PARSE_MODEL = Bool.parseBool(Config.get(Config.INTERLINGUAL_TRAIN_PARSE_MODEL));
			TRAIN_GENERATE_MODEL = Bool.parseBool(Config.get(Config.INTERLINGUAL_TRAIN_GENERATE_MODEL));
		}
		mtConfig = Config.getFilename();
		mtModelDir = Config.getModelDir();
		parseConfig = Config.get(Config.INTERLINGUAL_PARSE_CONFIG);
		genConfig = Config.get(Config.INTERLINGUAL_GEN_CONFIG);
		Config.read(parseConfig);
		srcNL = Config.getSourceNL();
		File pd = new File(mtModelDir, srcNL+"-"+Config.getMRL());
		pd.mkdirs();
		parseModelDir = pd.getPath();
		Config.setModelDir(parseModelDir);
		pm = ParseModel.createNew();
		Config.read(genConfig);
		tarNL = Config.getTargetNL();
		File gd = new File(mtModelDir, Config.getMRL()+"-"+tarNL);
		gd.mkdirs();
		genModelDir = gd.getPath();
		Config.setModelDir(genModelDir);
		gm = GenerateModel.createNew();
		Config.read(mtConfig);
		Config.setModelDir(mtModelDir);
	}
	
	public Translator getTranslator() throws IOException {
		return new InterlingualTranslator(pm, gm);
	}

	public void train(Examples examples) throws IOException {
		if (DO_CROSS_VALIDATE) {
			Examples[][] splits = examples.crossValidate(NUM_FOLDS);
			Examples out = new Examples();
			Config.read(parseConfig);
			Config.setModelDir(parseModelDir);
			NL.useNL = srcNL;
			for (int i = 0; i < NUM_FOLDS; ++i) {
				ParseModel pm = ParseModel.createNew();
				pm.train(splits[i][0], true);
				//FIXME does it need to be read?
				//pm.read();
				Parser parser = pm.getParser();
				for (int j = 0; j < splits[i][1].size(); ++j) {
					Example ex = splits[i][1].getNth(j);
					Iterator kt = parser.parse(ex.E());
					if (kt.hasNext()) {
						Parse parse = (Parse) kt.next();
						ex.F = new Meaning(parse.toStr());
						out.add(ex);
					}
				}
			}
			Config.read(genConfig);
			Config.setModelDir(genModelDir);
			NL.useNL = tarNL;
			gm.train(out);
			if (TRAIN_PARSE_MODEL) {
				Config.read(parseConfig);
				Config.setModelDir(parseModelDir);
				NL.useNL = srcNL;
				pm.train(examples);
			}
		} else {
			if (TRAIN_PARSE_MODEL) {
				Config.read(parseConfig);
				Config.setModelDir(parseModelDir);
				NL.useNL = srcNL;
				pm.train(examples);
			}
			if (TRAIN_GENERATE_MODEL) {
				Config.read(genConfig);
				Config.setModelDir(genModelDir);
				NL.useNL = tarNL;
				gm.train(examples);
			}
		}
		Config.read(mtConfig);
		Config.setModelDir(mtModelDir);
	}

	public void read() throws IOException {
		Config.read(parseConfig);
		Config.setModelDir(parseModelDir);
		pm.read();
		Config.read(genConfig);
		Config.setModelDir(genModelDir);
		gm.read();
		Config.read(mtConfig);
		Config.setModelDir(mtModelDir);
	}

}
