package dev.orbonly;

public enum OrbLabelPosition
{
	CENTER("Center"),
	FREE("Free (Alt-drag)");

	private final String name;

	OrbLabelPosition(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
